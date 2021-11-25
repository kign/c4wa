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

const wasm_printf = function (wasm_mem) {
    return function(offset, argc) {
        wasm_mem_fprintf(wasm_mem, process.stdout, offset, argc);
    }
}

const wasm_fprintf = function (wasm_mem, target) {
    return function(offset, argc) {
        wasm_mem_fprintf(wasm_mem, target, offset, argc);
    }
}

const wasm_mem_fprintf = function (wasm_mem, target, offset, argc) {
    const mem = wasm_mem ();

    const p_fmt = read_i32(mem, offset);
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

            if (fmt[i] === 'l') {
                fmt[i] = null;
                i++;
            }
            offset += 8;
            if ('cdxu'.includes(fmt[i])) {
                const r = read_i64(mem, offset);
                if (fmt[i] === 'c')
                    args.push(String.fromCharCode(Number(r)));
                else if (-(2n ** 53n) < r && r < 2n ** 53n)
                    args.push(Number(r));
                else {
                    // bad hack: loosing all format specifiers
                    args.push(r.toString());
                    fmt[i] = 's';
                }
            } else if (fmt[i] === 'f')
                args.push(read_f64(mem, offset));
            else if (fmt[i] === 's') {
                const s = read_str(mem, Number(read_i64(mem, offset)));
                args.push(s);
            } else {
                console.error("Format '" + fmt[i] + "' not known at this time");
                return;
            }
        }
    }

    if (args.length + 1 !== argc) {
        console.error("", "Format string '" + fmt + "' expected", args.length,
            "substitutions, passed", argc - 1, "arguments");
        return;
    }
    const res = printf(fmt.filter(x => x !== null).join('').replaceAll('\\n', '\n'), ...args);

    if (target.write)
        target.write(res);
    else if (target.push)
        target.push(res);
}

module.exports = {
    wasm_printf  : wasm_printf,
    wasm_fprintf : wasm_fprintf
};

