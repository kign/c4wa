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
coding targeting `c4wa` isn't much more complicated than coding in standard C. 
It supports loops, conditionals, block scope of
variables, all of C operators and primitive types, `struct`s, arrays, pointer, variable arguments
and dynamic memory allocation. 
It can also apply external C preprocessor to your code before parsing.

## Installation

Download last release from [here](https://github.com/kign/c4wa/releases/); unzip to any directory
and use shell wrapper `c4wa-compile` 

```bash
mkdir -p ~/Apps
cd ~/Apps
wget https://github.com/kign/c4wa/releases/download/v0.3/c4wa-compile-0.3.zip
unzip c4wa-compile-0.3.zip
cd
PATH=~/Apps/c4wa-compile-0.3/bin:$PATH
c4wa-compile --help
```

## Usage

Let's say we want to check [Collatz conjecture](https://en.wikipedia.org/wiki/Collatz_conjecture) for a 
given integer number _N_.

We start from this C code, which we save to file `collatz.c` :

```c
extern int collatz(int N) {
    int len = 0;
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
function `printf` similarly to how you would in C; it also automatically calls function `main` (with no arguments).
You can use it with any of the tests in [this directory](https://github.com/kign/c4wa/tree/master/src/test/resources/c). 
For example

```bash
c4wa-compile 170-life.c
wat2wasm --enable-bulk-memory  170-life.wat
etc/run-wasm 170-life.wasm
```

See [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md) 
for in-depth discussion of implementing `printf` in WASM environment, 
and also the [source code](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js).

## Examples

### Test suite

There is large (and growing) set of tests, from trivial to rather complicated, in 
[this directory](https://github.com/kign/c4wa/tree/master/src/test/resources/c).
For each of these files, you can find generated WAT code [here](https://github.com/kign/c4wa/tree/master/tests/wat).

### Game of life

Previously, I wrote a native WAT implementation for Conway's game of life (on a final toroidal board); 
later I used original implementation in C and compiled with `c4wa`.

  * [Original implementation in C](https://github.com/kign/life/blob/master/lib/lifestep.c)
  * [Original and independent implementation in WAT](https://github.com/kign/life/blob/master/wasm/life.wat)
  * [C source adapted for `c4wa`](https://github.com/kign/life/blob/master/wasm/life-wasm.c) (note: this was based on release 0.1 of the compiler, stack variables were not yet available)
  * [WAT compiled from the above C source](https://github.com/kign/life/blob/master/wasm/life-wasm.wat)

**Conclusions**:

  * Only minimal changes to the code were necessary for make it compatible with `c4wa` (and some of these changes wouldn't be necessary in version 0.2);
  * `c4wa` compiler yields comparable though a bit larger WASM file (1415 bytes vs 1187);
  * Performance of `c4wa`-generated implementation is pretty much same as the original implementation directly in WAT, 
    except for `wasmer` runtime, where it is significantly better.

## Documentation

 * [Comparison with `emscripten` and other compilers](https://github.com/kign/c4wa/blob/master/etc/doc/comparison.md)
 * [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md)
 * [Compiler configuration options](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md)

## Development and testing

To run tests, execute

```bash
./gradlew test
etc/run-tests all
```

First command will run all units tests; **this only verifies successful compilation, not correctness of generated code**.
node.js-based script `run-tests` will then run `wat2wasm` on every created WAT file and will verify that 
generated WASM would run and print expected output (saved as commented out section in every source file).
It will also cross-compile with native C and check that output is exactly the same.

Due to this multistage process C Source => WAT => WASM => execute, there could be three types of changes you are
making:

  1. Changes which are NOT expected to update any of the existing WAT files. For example, you could 
     be optimizing or cleaning the code, or implementing a new language feature;
  2. Changes which are expected to propagate to (some) WAT files, but not actually change generated WASM. 
     This is relatively rare, but for example you may be changing variable naming or formatting;
     You'll see updates in WAT, but none of that has any impact on the output of `wat2wasm`;
  3. Finally, your changes could be expected to actually change (hopefully, improve or optimize) generated WASM code.

After running `./gradlew test` you should look at updated WAT files in 
`tests/wat` [directory](https://github.com/kign/c4wa/tree/master/tests/wat) whether anything changed unexpectedly.
If not (and wasn't expected to, case 1 above), there isn't anything else to test.

If there _are_ changes, you should first compare new versions of updated WAT files to approve the changes.
If changed are as expected, _then_ you could run `etc/run-tests all`. It'll do two things for you:

  * Run all WAT files through `wat2wam` to create WASM files in 
    `tests/wasm` [directory](https://github.com/kign/c4wa/tree/master/tests/wasm);
  * Load these WASM files in `node.js` and execute function `main` in each one of them.

If you are making changes of type 2, at this point you need to make sure WASM files haven't changed and 
if they indeed haven't, you are all set. If they did change, and were expected to, you need to pay attention
to the report generated by `run-tests` to make sure all tests actually passed runtime execution.

