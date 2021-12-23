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