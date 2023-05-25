from fastapi import FastAPI
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
import ctypes

lib = ctypes.CDLL('./run_raqm.so')

lib.runRaqm.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_size_t]
lib.runRaqm.restype = None

def is_rtl_language(text):
    for char in text:
        if unicodedata.bidirectional(char) in ('R', 'AL'):
            return True
    return False

def get_glyphs(font_path, text):
    fontfile = font_path.encode('utf-8')
    text = text.encode('utf-8')
    direction = b"ltr"
    language = b"en"

    buffer_size = 2 ** 12
    buffer = ctypes.create_string_buffer(buffer_size)

    lib.runRaqm(fontfile, text, direction, language, buffer, buffer_size)
    output = buffer.value.decode('utf-8')

    if output == '\n':
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
        # unicode codepoint decimal number: list of font path strings
    }

    font_paths = []
    # font_paths.extend(glob("/usr/share/fonts/truetype/**/*.ttf", recursive=True))
    font_paths.extend(glob("/usr/share/fonts/truetype/noto/*", recursive=True))
    # font_paths.extend(glob("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"))

    for font_path in font_paths:
        print('reading', font_path)
        with TTFont(
            font_path, 0, allowVID=0, ignoreDecompileErrors=True, fontNumber=-1
        ) as ttf:
            chars = chain.from_iterable(
                [y + (Unicode[y[0]],) for y in x.cmap.items()] for x in ttf["cmap"].tables
            )
            for c in chars:
                if c[0] in result:
                    if font_path not in result[c[0]]:
                        result[c[0]].append(font_path)
                else:
                    result[c[0]] = [font_path]

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

def find_fonts(text):
    font_to_character_count = {}
    for letter in text:
        if ord(letter) in unicode_to_font_path:
            for font_path in unicode_to_font_path[ord(letter)]:
                if font_path in font_to_character_count:
                    font_to_character_count[font_path] += 1
                else:
                    font_to_character_count[font_path] = 1
    
    complete_fonts = []
    for font_path in font_to_character_count:
        if font_to_character_count[font_path] == len(text):
            complete_fonts.append(font_path)

    filtered_fonts = []
    for font_path in complete_fonts:
        if 'UI' in font_path:
            continue
        if 'Mono' in font_path:
            continue
        if 'Italic' in font_path:
            continue
        if 'Display' in font_path:
            continue
        if 'Bold' in font_path:
            continue
        if 'Serif' in font_path:
            continue
        filtered_fonts.append(font_path)
    
    if len(filtered_fonts) > 0:
        return filtered_fonts
    
    if len(complete_fonts) > 0:
        return complete_fonts
    
    # maybe it is a CJK string

    cjk_font_path = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
    if get_glyphs(cjk_font_path, text) is not None:
        return [cjk_font_path]
    
    return []

def find_font(text):
    fonts = find_fonts(text)

    if len(fonts) == 0:
        return None
    
    return fonts[0]

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

def segment_word(font_path, text):
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

def segment_label(text):
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
            word_parts = segment_word(font_path, word)
        
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
        parts = segment_label(text)
        if parts:
            encoded_result = urllib.parse.quote(''.join(["@".join(part) for part in parts]))
            add_to_cache(encoded_request, encoded_result)

    print('duration', time.time() - tic)
    return encoded_result

# if __name__ == "__main__":
#     uvicorn.run(app, host="0.0.0.0", port=3000)


# font_path = '/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf'
# text = 'モナコ'

# print(find_font(text))

# tic = time.time()
# for _ in range(1000):
#     segment_word(font_path, text)
# print(time.time() - tic)

text = 'મુંબઈ मुंबई دبي-بي Oliver'  # 'ᄀᄀᄀ각ᆨᆨ' # 'دبي' # 'મુંબઈ' # 'मुंबई' #'oliver' # 'モナコ'

parts = segment_label(text)

if parts is not None:
    for part in parts:
        print(part)
