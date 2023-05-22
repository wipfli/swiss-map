from fastapi import FastAPI, Request
import uvicorn
from fastapi.responses import HTMLResponse



import subprocess
from glob import glob

from itertools import chain

from fontTools.ttLib import TTFont
from fontTools.unicode import Unicode

import os

import json

import unicodedata

import urllib.parse

import re

import time

def is_rtl_language(text):
    for char in text:
        if unicodedata.bidirectional(char) in ('R', 'AL'):
            return True
    return False

def read_cli_output(cli_command):
    try:
        output = subprocess.check_output(cli_command, shell=True, text=True)
        return output.strip()
    except subprocess.CalledProcessError as e:
        # print(f"Error executing CLI command: {e}")
        return None

def get_glyphs(font_path, text):
    cli_command = f"./run_raqm {font_path} \"{text}\" ltr en"
    output = read_cli_output(cli_command)
    if output is None:
        return None
    glyphs = [line_to_glyph(line) for line in output.splitlines()]
    if 0 in [glyph["index"] for glyph in glyphs]:
        return None
    return glyphs

def line_to_glyph(line):
    index, x_offset, y_offset, x_advance, y_advance, cluster = [int(num) for num in line.split()]
    return {
        "index": index,
        "x_offset": x_offset,
        "y_offset": y_offset,
        "x_advance": x_advance,
        "y_advance": y_advance,
        "cluster": cluster,
    }

def build_unicode_to_font_path():
    result = {
        # unicode codepoint decimal number: font path string
    }

    font_paths = []
    font_paths.extend(glob("/usr/share/fonts/truetype/**/*.ttf", recursive=True))
    font_paths.extend(glob("/usr/share/fonts/truetype/noto/*Regular.ttf", recursive=True))
    font_paths.extend(glob("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"))

    for font_path in font_paths:
        print('reading', font_path)
        with TTFont(
            font_path, 0, allowVID=0, ignoreDecompileErrors=True, fontNumber=-1
        ) as ttf:
            chars = chain.from_iterable(
                [y + (Unicode[y[0]],) for y in x.cmap.items()] for x in ttf["cmap"].tables
            )
            for c in chars:
                result[c[0]] = font_path

    return result

def find_font_with_raqm(folder, text):
    for font_path in glob(folder + "/**/*", recursive=True):
        glyphs = get_glyphs(font_path, text)
        if glyphs is not None:
            return font_path
    return None

unicode_to_font_path = {}

filename = 'unicode_to_font_path.json'
if os.path.exists(filename):
    with open(filename) as f:
        data = json.load(f)
        for key, value in data.items():
            unicode_to_font_path[int(key)] = value
else:
    unicode_to_font_path = build_unicode_to_font_path()
    with open(filename, 'w') as f:
        json.dump(unicode_to_font_path, f)

def find_font(text):
    fonts = []

    try:
        for letter in text:
            font_path = unicode_to_font_path[ord(letter)]
            if font_path not in fonts:
                fonts.append(font_path)
    except KeyError:
        pass

    if len(fonts) == 1:
        return fonts[0]
    else:
        # print('len fonts', len(fonts))
        # print(fonts)
        font = find_font_with_raqm('/usr/share/fonts/opentype', text)
        if font:
            print('raqm find font', font)
            return font
        font = find_font_with_raqm('/usr/share/fonts', text)
        if font:
            #print(font)
            return font
    
    return None


def can_break_after(font_path, text, break_after):

    glyphs = get_glyphs(font_path, text)
    # pprint(glyphs)

    text_before = text[0:(break_after + 1)]
    text_after = text[(break_after + 1):]
        
    # print(text_before, text_after)

    glyphs_before = get_glyphs(font_path, text_before)
    if glyphs_before is None:
        return False
    
    glyphs_after = get_glyphs(font_path, text_after)
    if glyphs_after is None:
        return False

    max_cluster_before = max([glyph['cluster'] for glyph in glyphs_before])

   
    
    for glyph in glyphs_after:
        glyph['cluster'] += max_cluster_before + 1

    if is_rtl_language(text):
        glyphs_broken = glyphs_after + glyphs_before
    else:
        glyphs_broken = glyphs_before + glyphs_after

    # pprint(glyphs_broken)

    if len(glyphs) != len(glyphs_broken):
        # print('not same length')
        return False
    
    properties = ['index', 'x_advance', 'x_offset', 'y_advance', 'y_offset']
    last_cluster = -1
    last_cluster_broken = -1
    for i in range(len(glyphs)):
        for property in properties:
            if glyphs[i][property] != glyphs_broken[i][property]:
                # print('not same property', i, property, glyphs[i][property], glyphs_broken[i][property])
                return False
        if glyphs[i]['cluster'] != last_cluster or glyphs_broken[i]['cluster'] != last_cluster_broken:
            if glyphs[i]['cluster'] == last_cluster or glyphs_broken[i]['cluster'] == last_cluster_broken:
                # print('cannot break i', i)
                return False
            last_cluster = glyphs[i]['cluster']
            last_cluster_broken = glyphs_broken[i]['cluster']
    
    return True

    
# print(font_path)

# print('can break after', break_after, can_break_after(font_path, text, break_after))

def extract_parts(font_path, text):
    cursor = 0
    parts = []
    rtl = is_rtl_language(text)
    for break_after in range(len(text)):
        if can_break_after(font_path, text, break_after):
            if rtl:
                parts.insert(0, text[cursor:(break_after + 1)])
            else:
                parts.append(text[cursor:(break_after + 1)])
            
            cursor = break_after + 1
    return parts


def split_words(string):
    # Define the pattern to match special characters (+ or -)
    pattern = r'([\?\.\,\+\-\*/\^=<>!&\|\(\)\[\]\{\}\'\s])'

    # Use the re.split() function to split the string based on the pattern
    split_strings = re.split(pattern, string)

    # Return the split strings
    return split_strings

def extract_parts_multi(text):
    words = split_words(text)

    parts = []

    directional_parts = []

    if len(words) == 0:
        return None

    previous_rtl = not is_rtl_language(words[0])

    for word in words:
        font_path = find_font(word)
        word_parts = []
        if font_path is None:
            word_parts = [word]
        else:
            word_parts = extract_parts(font_path, word)
        
        rtl = is_rtl_language(word)
        if rtl == previous_rtl:
            if rtl:
                directional_parts = word_parts  + directional_parts
            else:
                directional_parts = directional_parts + word_parts
        else:
            parts += directional_parts
            directional_parts = word_parts
        
        previous_rtl = rtl

    parts += directional_parts

    return parts





#############################################

app = FastAPI()

cache_filename = 'cache.csv'

cache = {}

if os.path.exists(cache_filename):
    print('reading cache')
    with open(cache_filename) as f:
        for line in f.readlines():
            line = line.strip()
            encoded_request, encoded_result = line.split(',')
            cache[encoded_request] = encoded_result

def add_to_cache(encoded_request, encoded_result):
    if encoded_request not in cache:
        cache[encoded_request] = encoded_result
        with open(cache_filename, 'a') as f:
            f.write(f'{encoded_request},{encoded_result}\n')

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.get("/segment", response_class=HTMLResponse)
async def segment(text: str):

    encoded_request = urllib.parse.quote(text)
    print('requested', text, encoded_request)

    if encoded_request in cache:
        print('cache hit')
    else:
        print('cache miss')

    tic = time.time()
    
    encoded_result = ''

    if encoded_request in cache:
        encoded_result = cache[encoded_request]
    else:
        parts = extract_parts_multi(text)
        if parts:
            encoded_result = urllib.parse.quote(''.join(["@".join(part) for part in parts]))
            add_to_cache(encoded_request, encoded_result)

    print('duration', time.time() - tic)
    return encoded_result

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=3000)
