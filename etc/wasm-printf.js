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
}

module.exports = {
    wasm_printf  : wasm_printf,
    read_i32 : read_i32
};

