# C compiler for Web Assembly (`c4wa`)

This a compiler from a subset of C language to
[Web Assembly Text format](https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format).
WAT files could subsequently be compiled to Web Assembly with a `wat2wasm` tool.

## Motivation

There are [many existing compilers](https://github.com/appcypher/awesome-wasm-langs)
from various programming languages to Web Assembly, including popular
[`emscripten`](https://github.com/emscripten-core/emscripten) for compiling C code. They typically treat
Web Assembly as a target not too different from normal machine-level assembly; their main advantage is
full support of the underlying language (so you can compile your existing code base with few, if any, changes),
but in the process they often create bloated, unnecessary, and poorly fitting Web Assembly design code.

You may, of course, not care, as long as at the end it's working as expected. Some people who do care
choose to write relatively simple fragments of Web Assembly in WAT (text-based) format. To make it clear,
WAT format is more than just Web Assembly instructions written as text; it supports S-expressions and
some other syntax sugar to make coding easier (See
excellent [introduction](https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format)
to WAT format at MDN.) Still, you are required to write each and every Web Assembly instructions manually, 
so that for example a simple assignment like this: `c = a*a + b*b + 1` might look like this: 
 
```wat
(set_local $c (i32.add (i32.add (i32.mul (get_local $a) (get_local $a)) 
                    (i32.mul (get_local $b) (get_local $b))) (i32.const 1)))
```

`c4wa` purports to be a middle ground between these two extremes. It allows you to write your code in a 
relatively higher-level language (a subset of `C`) while retaining a close relation to an underlying
Web Assembly. Instead of a binary WASM file, it generates a well-formatted WAT output 
which is trying to be similar to what a human programmer would have written when solving the problem directly in WAT.

`c4wa` is not a full C implementation and isn't trying to be one. Still, most of the typical day-to-day
coding in `c4wa` isn't much more complicated than coding in standard C. It supports loops, conditionals,
all of C operators, `struct`s, arrays and pointers. It can also optionally apply external C preprocessor to your code
before parsing.  

## Installation

Download last release from [here](https://github.com/kign/c4wa/releases/); unzip to any directory
and use shell wrapper `c4wa-compile` 

```bash
mkdir -p ~/Apps
cd ~/Apps
wget https://github.com/kign/c4wa/releases/download/v0.1/c4wa-compile-0.1.zip
unzip c4wa-compile-0.1.zip
cd
PATH=~/Apps/c4wa-compile-0.1/bin:$PATH
c4wa-compile --help
```

## Usage

Let's say we want to check [Collatz conjecture](https://en.wikipedia.org/wiki/Collatz_conjecture) for a 
given integer number _N_.

We start from this C code, which we save to file `collatz.c` :

```c
extern int collatz(int N) {
    int len;
    unsigned long n = (unsigned long) N;
    do {
        if (n == 1)
            break;
        if (n % 2 == 0)
            n /= 2;
        else
            n = 3 * n + 1;
        len ++;
    }
    while(1);
    return len;
}
```

Use `c4wa-compile` to compile to WAT and then `wat2wasm` to compile to WASM:

```bash
c4wa-compile -Xmodule.memoryStatus=none collatz.c
wat2wasm collatz.wat
```

Write this simple `node`-based wrapper (save it as file `collatz.js`)

```javascript
const fs = require('fs');
const wasm_bytes = new Uint8Array(fs.readFileSync('collatz.wasm'));
const n = parseInt(process.argv[2]);
WebAssembly.instantiate(wasm_bytes).then(wasm =>
    console.log("Cycle length of", n, "is", wasm.instance.exports.collatz (n)))
```

Now you can run the code :

```bash
node collatz.js 626331
# Output: Cycle length of 626331 is 508
```

Note that generated WASM file `collatz.wasm` **is only 99 bytes in size**.

There is nothing whatsoever that forces you to use `node` or JavaScript to execute WASM files.
There are many universal runtimes with bindings available for many languages. For example, 
using [wasmer](https://wasmer.io/), you can run `collatz.wasm` in python with this simple
script:

```python
import sys
from wasmer import engine, Store, Module, Instance
from wasmer_compiler_llvm import Compiler

store = Store(engine.Native(Compiler))
module = Module(store, open('collatz.wasm', 'rb').read())
instance = Instance(module)

n = int(sys.argv[1]);
print("Cycle length of", n, "is", instance.exports.collatz(n))
```

Save it as `collatz.py`, install `wasmer` bindings and execute:

```bash
python3 -m pip install --upgrade wasmer wasmer_compiler_llvm
python3 collatz.py 626331
# Cycle length of 626331 is 508
```

We also provide a customized `node`-based runtime for testing, which allows you to use inside your code
function `printf` very similar to how you would in C; it automatically calls function `main` (with no arguments).
You can use it with any of the tests in [this directory](https://github.com/kign/c4wa/tree/master/src/test/resources/c). 
For example

```bash
c4wa-compile -P 170-life.c
wat2wasm --enable-bulk-memory  170-life.wat
etc/run-wasm 170-life.wasm
```

See [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md) 
for in-depth discussion of implementing `printf` in WASM environment, 
and also the [source code](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js).

## Documentation

 * [Comparison with `emscripten` and other compilers](https://github.com/kign/c4wa/blob/master/etc/doc/comparison.md)
 * [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md)
 * [Compiler configuration options](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md)