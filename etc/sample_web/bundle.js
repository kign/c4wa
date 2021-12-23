(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
const {wasm_printf} = require('../wasm-printf');

function main() {
    const elm_html_dbg = document.getElementById('dbg');
    const elm_n = document.getElementById('n');
    const elm_prime = document.getElementById("prime");
    const dbg_write = s => {
        elm_html_dbg.value += s;
    }

    let wasm_mem;
    const wasm_src = 'prime.wasm';

    fetch(wasm_src).then(response => {
        if (response.status !== 200)
            alert(`File ${wasm_src} returned status ${response.status}`);
        return response.arrayBuffer();
    }).then(bytes => {
        return WebAssembly.instantiate(bytes, {
            c4wa: {
                printf: wasm_printf(() => new Uint8Array(wasm_mem.buffer), x => dbg_write(x))
            }
        });
    }).then(wasm => {
        dbg_write("Loaded " + wasm_src + "\n");
        const e = wasm.instance.exports;
        wasm_mem = e.memory;
        elm_n.addEventListener('change', evt => {
          const res = e.nth_prime(parseInt(evt.target.value));
          elm_prime.innerHTML = res;

        });
    });
}

main();
},{"../wasm-printf":2}],2:[function(require,module,exports){
const printf = require('fast-printf').printf;

const read_i32 = function (mem, offset) {
    // not really necessary, we can use Number(read_i64(...))
    let val = 0;

    for (let i = 3; i >= 0; i--)
        val = 256 * val + mem[offset + i];
    if (mem[offset + 3] >= 128)
        val -= 2 ** 32;
    return val;
}

const read_i64 = function (mem, offset) {
    // have to use BigInt, since native Number doesn't have sufficient precision to support
    // full 64-bit integer arithmetic (it works up to 53 binary digits).
    let val = 0n;

    if (mem[offset + 7] >= 128) {
        for (let i = 7; i >= 0; i--)
            val = 256n * val + BigInt(255 ^ mem[offset + i]);
        val = -val - 1n;
    } else {
        for (let i = 7; i >= 0; i--)
            val = 256n * val + BigInt(mem[offset + i]);
    }
    return val;
}

const read_f64 = function (mem, offset) {
    return new Float64Array(mem.slice(offset, offset + 8).buffer)[0];
}

const read_str = function (mem, offset) {
    const bytes = new Uint8Array(1024);
    let i = 0;
    while (i < 1024) {
        const c = mem[offset + i];
        if (c === 0)
            return new TextDecoder().decode(bytes.slice(0, i));
        bytes[i++] = c;
    }
    return null;
}

const wasm_printf = function (wasm_mem, consumer) {
    return function(fmt, offset) {
        wasm_mem_printf(fmt, offset, wasm_mem, consumer);
    }
}

const wasm_mem_printf = function (p_fmt, offset, wasm_mem, consumer) {
    // console.log(_fmt, offset, wasm_mem, target);
    const mem = wasm_mem ();

    const fmt = read_str(mem, p_fmt).split('');

    const args = [];
    for (let i = 0; i < fmt.length - 1; i++) {
        if (fmt[i] === '%') {
            i++;
            if (fmt[i] === '%')
                continue;
            while (i < fmt.length && !('a' <= fmt[i] && fmt[i] <= 'z' || 'A' <= fmt[i] && fmt[i] <= 'Z'))
                i++;
            if (i === fmt.length) {
                console.error("Invalid format string", fmt);
                return;
            }

            const is_long = fmt[i] === 'l';
            if (fmt[i] === 'l') {
                fmt[i] = null;
                i++;
            }
            if ('cdxu'.includes(fmt[i])) {
                const r = read_i64(mem, offset);
                if (fmt[i] === 'c')
                    args.push(String.fromCharCode(Number(r)));
                else if (-(2n ** 53n) < r && r < 2n ** 53n && (!'xu'.includes(fmt[i]) || (r >= 0n && r < 4294967296n)))
                    args.push(Number(r));
                else {
                    // bad hack: loosing all format specifiers
                    if (r < 0n && 'xu'.includes(fmt[i]))
                        args.push((r + (is_long? 2n ** 64n : 2n ** 32n)).toString(fmt[i] === 'x'? 16: 10));
                    else if (r >= 4294967296n && 'xu'.includes(fmt[i]))
                        args.push(r.toString(fmt[i] === 'x'? 16: 10));
                    else
                        args.push(r.toString());
                    fmt[i] = 's';
                }
            } else if ("feE".includes(fmt[i]))
                args.push(read_f64(mem, offset));
            else if (fmt[i] === 's') {
                const s = read_str(mem, Number(read_i64(mem, offset)));
                args.push(s);
            } else {
                console.error("Format '" + fmt[i] + "' not known at this time");
                return;
            }

            offset += 8;
        }
    }

    const res = printf(fmt.filter(x => x !== null).join(''), ...args);

    if (consumer)
        consumer(res);
/*
    if (!target)
        console.log(res.trim());
    else if (target.write)
        target.write(res);
    else if (target.push)
        target.push(res);
*/
}

module.exports = {
    wasm_printf  : wasm_printf
};


},{"fast-printf":4}],3:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createPrintf = void 0;
const boolean_1 = require("boolean");
const tokenize_1 = require("./tokenize");
const formatDefaultUnboundExpression = (
// @ts-expect-error unused parameter
subject, token) => {
    return token.placeholder;
};
const createPrintf = (configuration) => {
    var _a;
    const padValue = (value, width, flag) => {
        if (flag === '-') {
            return value.padEnd(width, ' ');
        }
        else if (flag === '-+') {
            return ((Number(value) >= 0 ? '+' : '') + value).padEnd(width, ' ');
        }
        else if (flag === '+') {
            return ((Number(value) >= 0 ? '+' : '') + value).padStart(width, ' ');
        }
        else if (flag === '0') {
            return value.padStart(width, '0');
        }
        else {
            return value.padStart(width, ' ');
        }
    };
    const formatUnboundExpression = (_a = configuration === null || configuration === void 0 ? void 0 : configuration.formatUnboundExpression) !== null && _a !== void 0 ? _a : formatDefaultUnboundExpression;
    const cache = {};
    // eslint-disable-next-line complexity
    return (subject, ...boundValues) => {
        let tokens = cache[subject];
        if (!tokens) {
            tokens = cache[subject] = tokenize_1.tokenize(subject);
        }
        let result = '';
        for (const token of tokens) {
            if (token.type === 'literal') {
                result += token.literal;
            }
            else {
                let boundValue = boundValues[token.position];
                if (boundValue === undefined) {
                    result += formatUnboundExpression(subject, token, boundValues);
                }
                else if (token.conversion === 'b') {
                    result += boolean_1.boolean(boundValue) ? 'true' : 'false';
                }
                else if (token.conversion === 'B') {
                    result += boolean_1.boolean(boundValue) ? 'TRUE' : 'FALSE';
                }
                else if (token.conversion === 'c') {
                    result += boundValue;
                }
                else if (token.conversion === 'C') {
                    result += String(boundValue).toUpperCase();
                }
                else if (token.conversion === 'i' || token.conversion === 'd') {
                    boundValue = String(Math.trunc(boundValue));
                    if (token.width !== null) {
                        boundValue = padValue(boundValue, token.width, token.flag);
                    }
                    result += boundValue;
                }
                else if (token.conversion === 'e') {
                    result += Number(boundValue)
                        .toExponential();
                }
                else if (token.conversion === 'E') {
                    result += Number(boundValue)
                        .toExponential()
                        .toUpperCase();
                }
                else if (token.conversion === 'f') {
                    if (token.precision !== null) {
                        boundValue = Number(boundValue).toFixed(token.precision);
                    }
                    if (token.width !== null) {
                        boundValue = padValue(String(boundValue), token.width, token.flag);
                    }
                    result += boundValue;
                }
                else if (token.conversion === 'o') {
                    result += (Number.parseInt(String(boundValue), 10) >>> 0).toString(8);
                }
                else if (token.conversion === 's') {
                    if (token.width !== null) {
                        boundValue = padValue(String(boundValue), token.width, token.flag);
                    }
                    result += boundValue;
                }
                else if (token.conversion === 'S') {
                    if (token.width !== null) {
                        boundValue = padValue(String(boundValue), token.width, token.flag);
                    }
                    result += String(boundValue).toUpperCase();
                }
                else if (token.conversion === 'u') {
                    result += Number.parseInt(String(boundValue), 10) >>> 0;
                }
                else if (token.conversion === 'x') {
                    boundValue = (Number.parseInt(String(boundValue), 10) >>> 0).toString(16);
                    if (token.width !== null) {
                        boundValue = padValue(String(boundValue), token.width, token.flag);
                    }
                    result += boundValue;
                }
                else {
                    throw new Error('Unknown format specifier.');
                }
            }
        }
        return result;
    };
};
exports.createPrintf = createPrintf;

},{"./tokenize":5,"boolean":6}],4:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.printf = exports.createPrintf = void 0;
const createPrintf_1 = require("./createPrintf");
Object.defineProperty(exports, "createPrintf", { enumerable: true, get: function () { return createPrintf_1.createPrintf; } });
exports.printf = createPrintf_1.createPrintf();

},{"./createPrintf":3}],5:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.tokenize = void 0;
const TokenRule = /(?:%(?<flag>([+0-]|-\+))?(?<width>\d+)?(?<position>\d+\$)?(?<precision>\.\d+)?(?<conversion>[%BCESb-iosux]))|(\\%)/g;
const tokenize = (subject) => {
    let matchResult;
    const tokens = [];
    let argumentIndex = 0;
    let lastIndex = 0;
    let lastToken = null;
    while ((matchResult = TokenRule.exec(subject)) !== null) {
        if (matchResult.index > lastIndex) {
            lastToken = {
                literal: subject.slice(lastIndex, matchResult.index),
                type: 'literal',
            };
            tokens.push(lastToken);
        }
        const match = matchResult[0];
        lastIndex = matchResult.index + match.length;
        if (match === '\\%' || match === '%%') {
            if (lastToken && lastToken.type === 'literal') {
                lastToken.literal += '%';
            }
            else {
                lastToken = {
                    literal: '%',
                    type: 'literal',
                };
                tokens.push(lastToken);
            }
        }
        else if (matchResult.groups) {
            lastToken = {
                conversion: matchResult.groups.conversion,
                flag: matchResult.groups.flag || null,
                placeholder: match,
                position: matchResult.groups.position ? Number.parseInt(matchResult.groups.position, 10) - 1 : argumentIndex++,
                precision: matchResult.groups.precision ? Number.parseInt(matchResult.groups.precision.slice(1), 10) : null,
                type: 'placeholder',
                width: matchResult.groups.width ? Number.parseInt(matchResult.groups.width, 10) : null,
            };
            tokens.push(lastToken);
        }
    }
    if (lastIndex <= subject.length - 1) {
        if (lastToken && lastToken.type === 'literal') {
            lastToken.literal += subject.slice(lastIndex);
        }
        else {
            tokens.push({
                literal: subject.slice(lastIndex),
                type: 'literal',
            });
        }
    }
    return tokens;
};
exports.tokenize = tokenize;

},{}],6:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.boolean = void 0;
const boolean = function (value) {
    switch (Object.prototype.toString.call(value)) {
        case '[object String]':
            return ['true', 't', 'yes', 'y', 'on', '1'].includes(value.trim().toLowerCase());
        case '[object Number]':
            return value.valueOf() === 1;
        case '[object Boolean]':
            return value.valueOf();
        default:
            return false;
    }
};
exports.boolean = boolean;

},{}]},{},[1]);
