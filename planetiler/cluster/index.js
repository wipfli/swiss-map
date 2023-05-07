const { createCanvas } = require('canvas');
const fs = require('fs');

const customJoiningCharacter = '@'; // String.fromCharCode(0x7); // bell

const debug = false;

const lineHeight = 60;

// Create a new canvas
const canvas = createCanvas(400, 5 * lineHeight);
const ctx = canvas.getContext('2d');

// Set the background color
ctx.fillStyle = 'white';
ctx.fillRect(0, 0, canvas.width, canvas.height);

// Set the font properties
ctx.font = '24px Arial';


function shouldMerge(string1, string2) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.fillText(`${string1}${string2}`, 0, lineHeight);

    const parts = [string1, string2];
    let offset = 0;
    for (var part of parts) {
        ctx.fillText(part, offset, 3 * lineHeight);
        offset += ctx.measureText(part).width;
    }

    if (offset === 0) {
        // the total width is zero
        return false;
    }
    var imageData1 = ctx.getImageData(0, 0, offset, 2 * lineHeight);
    var imageData2 = ctx.getImageData(0, 2 * lineHeight, offset, 2 * lineHeight);

    var sum1 = 0;
    var sum2 = 0;
    for (var i = 0; i < imageData1.data.length; i++) {
        sum1 += imageData1.data[i];
        sum2 += imageData2.data[i];
    }
    if (sum1 === 0) {
        debug && console.log('string1string2 does not display anything')
        return false;
    }
    if (Math.abs(sum1 - sum2) / sum1 > 0.001) {
        debug && console.log(`the parts ${string1}, ${string2} should be merged, sum1=${sum1}, sum2=${sum2}, ratio=${Math.abs(sum1 - sum2) / sum1}`);
        return true;
    }

    debug && console.log(`the parts ${string1}, ${string2} should not be merged, sum1=${sum1}, sum2=${sum2}, ratio=${Math.abs(sum1 - sum2) / sum1}`);
    return false;

}

function merge(parts) {
    let i = 0;
    while (i < parts.length - 1) {
        if (shouldMerge(parts[i], parts[i + 1])) {
            debug && save(`${i}-${parts[i]}-${parts[i + 1]}.png`);
            parts.splice(i, 2, parts[i] + parts[i + 1]);
            i = 0;
            continue;
        }
        debug && save(`${i}-${parts[i]}-${parts[i + 1]}.png`);
        i++;
    }
    return parts;
}

function save(filename) {

    const base64Data = canvas.toDataURL().replace(/^data:image\/png;base64,/, '');

    fs.writeFileSync(filename, base64Data, 'base64');
}

function printParts(parts, filename) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    let offset = 0;
    for (var part of parts) {
        ctx.fillText(part, offset, 3 * lineHeight);
        offset += ctx.measureText(part).width;
    }
    save(filename);
}

function partsToMarkedString(parts) {
    let result = '';
    for (const part of parts) {
        result += [...part].join(customJoiningCharacter);
    }
    return result;
}

// "Hallo" -> ["H", "a", "l", "l", "o"]
// "H@all@o" -> ["Ha", "l", "lo"]
// "H@a@llo" -> ["Hal", "l", "o"]

function markedStringToParts(markedString) {
    const result = [];
    if (markedString.length === 0 || markedString[0] === customJoiningCharacter) {
        return result;
    }
    let currentPart = markedString[0];
    for (let i = 1; i < markedString.length; ++i) {
        if (markedString[i] === customJoiningCharacter) {
            continue;
        }
        if (markedString[i - 1] === customJoiningCharacter) {
            currentPart += markedString[i]
        } else {
            result.push(currentPart);
            currentPart = markedString[i]
        }
    }
    result.push(currentPart);
    return result;
}


// const tmp = 'क्‍';
// console.log('bell', String.fromCharCode(0x7))
// const zwj = tmp[2];
// const input = 'क्';
// const text = `${input[0]}${String.fromCharCode(0x7)}${input[1]}`; // 'क्ष';  //'क्‍'; //'បឹង​ទន្លេសាប'; // 'ក្រុងសៀមរាប'; // 'ถนนราชบพิธ'; // 'မြေတွေလှရွှေတေ'; // 'ক্রেসেন্ট লেক'; //'मुंबई'; //'မြန်မာOliverအက္ခရာ सुताजे'
// 
// 
// console.log(text.length)
// const parts = [...text];
// 
// printParts(parts, 'before.png');
// console.log('before', parts)
// 
// merge(parts);
// 
// printParts(parts, 'after.png');
// console.log('after', parts)
// 

const express = require('express');
const app = express();
const port = 3000;

app.use(express.json());

app.get('/get_marked_string/:input', (req, res) => {
    const input = req.params.input;
    const parts = [...input];
    merge(parts);
    const markedString = encodeURI(partsToMarkedString(parts));
    res.json(markedString);
});

app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
