# C compiler for Web Assembly (`c4wa`)

This is a compiler from a subset of C language to Web Assembly.

If you're not familiar with Web Assembly, check out [Wikipedia article](https://en.wikipedia.org/wiki/WebAssembly).
Web Assembly is a new universal executable format for the Web; it complements more traditional JavaScript
for computationally intensive tasks or if there is a need to port to Web existing code written in other languages.

There are many compilers targeting Web Assembly; see for example a comprehensive list 
[here](https://github.com/appcypher/awesome-wasm-langs). Why do we need another one?

Here are some unique features of `c4wa` :

 * It creates minimalistic well-optimized Web Assembly output without any "glue" to make it
   work with your application, without any embedded libraries, or any other overhead. This is simply 
   C code translated as efficiently as possible to WASM; nothing more
 * It is out of the box fully compatible with any WASM runtime; there are no dependencies on JavaScript or `node`
 * It can efficiently utilize WASM linear memory model, making it possible to write applications 
   with full dynamic memory allocation support and still only minimal overhead
 * In addition to binary WASM format, it can output text-based 
   [WAT format](https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format),
   which is entirely readable, properly formatted and could be used for better understanding
   inner workings of the compiler, edited manually, copied to separate WASM projects, or used for teaching/learning 
   Web Assembly and WAT format

`c4wa` is not a full C implementation and isn't trying to be one. Still, most of the typical day-to-day coding
targeting `c4wa` isn't much more complicated than coding in standard C. It supports loops, conditionals, block scope of variables, intermingled declarations, all of C operators and primitive types, `struct`s, 
arrays, pointers, variable arguments and dynamic memory
allocation. It can also apply external C preprocessor to your code before parsing.

## Motivation

There are [many existing compilers](https://github.com/appcypher/awesome-wasm-langs)
from various programming languages to Web Assembly, including popular
[`emscripten`](https://github.com/emscripten-core/emscripten) for compiling C code. They typically treat
Web Assembly as a target not too different from a machine-level assembly; their main advantage is
full support of the underlying language (so you can compile your existing code base with few, if any, changes),
but in the process they often create bloated, unnecessary, and poorly fitting Web Assembly design code.

You may, of course, not care, as long as at the end it's working as expected. Some people who do care
choose to write relatively simple fragments of Web Assembly in WAT (text-based) format. To make it clear,
WAT format is more than just Web Assembly instructions written as text; it supports S-expressions and
some other syntax sugar to make coding easier (See
excellent [introduction](https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format)
to WAT format at MDN.) Still, you are required to write each and every Web Assembly instructions manually, 
so a simple assignment like this: `c = a*a + b*b + 1` might look like that: 
 
```wat
(set_local $c (i32.add (i32.add (i32.mul (get_local $a) (get_local $a)) 
                    (i32.mul (get_local $b) (get_local $b))) (i32.const 1)))
```

`c4wa` purports to be a middle ground between these two extremes. It allows you to write your code in a 
relatively higher-level language (a subset of `C`) while retaining a close relation to an underlying
Web Assembly. In addition to a binary WASM file, it can generate a well-formatted WAT output 
which will be similar to what a human programmer would have written when solving the problem directly in WAT.

## Dependencies

`c4wa` needs Java 11 or above. Using preprocessor requires external C compiler (`gcc` is recommended).

While most of the testing tools and examples given below assume POSIX-based environment, compiler itself
should work on any platform with Java installed. Generated WASM files are, of course, platform-independent.
(WAT files will be created in a default text format for your platform).

In order to run Web Assembly, you need a runtime. Easiest runtime to use is `node`; there are also 
universal runtimes such as [wasmtime](https://wasmtime.dev/) and [wasmer](https://wasmer.io/) with bindings
for many languages. Any modern browser will also have a Web Assembly runtime built-in, though it is
a bit more complicated since you'd also need a local server. 

`cw4a` is entirely runtime-agnostic, though its testing framework is built on the top of `node`. 

Finally, if you are working with Web Assembly, you probably should have 
[WebAssembly Binary Toolkit](https://github.com/WebAssembly/wabt) handy;
it allows you to compile WAT files, verify a WASM file, dump its content by sections, and a lot more. 
However, it's not required.    

## Installation

Download the latest release from [here](https://github.com/kign/c4wa/releases/); unzip to any directory
and use shell wrapper `c4wa-compile`. For example,

```bash
mkdir -p ~/Apps
cd ~/Apps
wget https://github.com/kign/c4wa/releases/download/v0.4/c4wa-compile-0.4.zip
unzip c4wa-compile-0.4.zip
cd
PATH=~/Apps/c4wa-compile-0.4/bin:$PATH
c4wa-compile --help
```

## Usage

Let's say we want to check [Collatz conjecture](https://en.wikipedia.org/wiki/Collatz_conjecture) for a 
given integer number _N_.

We start from this C code, which we save to file `collatz.c` :

```c
extern int collatz(int N) {
    int len = 0;
    unsigned long n = N;
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

Use `c4wa-compile` to compile:

```bash
c4wa-compile -Xmodule.memoryStatus=none collatz.c
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

If you run compiler with option `-k`, it'll also save a WAT file, which looks like this:

```wat
(module
  (func $collatz (export "collatz") (param $N i32) (result i32)
    (local $len i32)
    (local $n i64)
    (set_local $n (i64.extend_i32_s (get_local $N)))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i64.eq (get_local $n) (i64.const 1)))
        (if (i64.eqz (i64.rem_u (get_local $n) (i64.const 2)))
          (then
            (set_local $n (i64.div_u (get_local $n) (i64.const 2))))
          (else
            (set_local $n (i64.add (i64.mul (i64.const 3) (get_local $n)) (i64.const 1)))))
        (set_local $len (i32.add (get_local $len) (i32.const 1)))
        (br $@block_1_continue)))
    (get_local $len)))
```

If you can read Web Assembly instructions, you can see how this corresponds to the
original C code, and it would seem reasonably close to how one would solve this problem directly in WAT.

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

We also provide two slightly customized wrappers to run WASM files: `node`-based and `python-wasmer`-based.
Both will automatically call `main` function (must be exported) and both will support C-compatible `printf`. 
You can use it with any of the tests in 
[this directory](https://github.com/kign/c4wa/tree/master/src/test/resources/c). 
For example

```bash
c4wa-compile 170-life.c
# both wrappers should make same output 
etc/run-wasm 170-life.wasm
etc/run-wasm.py 170-life.wasm
```

See [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md) 
for in-depth discussion of implementing `printf` in WASM environment, 
and also the [source code](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js).

## No standard library

Web Assembly is an _embedded language_; it is intended to be executed from a _runtime_ which interprets
Web Assembly instructions, perhaps compiles them into a native code (either ahead of time or JIT), and
handles all communications with OS, execution environment and the user. It could also optionally provide Web
Assembly with access to some library functions, via _import_ functionality.

From that standpoint, integrating any kind of standard library with `c4wa` compiler isn't practical. To the extent
Web Assembly code might need access to some library utilities (mathematical utilities such as `atan2`, for example), 
it is almost always better to simply import them from the runtime, and most of the time, there isn't any other choice
anyway, since all communication with the environment is done through the runtime. For example, in order to 
read from or write to files in Web Assembly, one needs to import from runtime something 
resembling `fopen` function (and of course some runtimes, such as browser, won't support this).

The only exceptions could be methods either already embedded into Web Assembly specification 
(such as `sqrt` or `memcpy`) or dealing with dynamic memory allocations (`malloc` and `free`), and
perhaps also some common utilities to work with memory and with strings. 

Accordingly, `c4wa` compiler exposes all methods already available in Web Assembly as _built-in functions_ 
and gives a choice of memory managers with number of _built-in libraries_, and that's about it.
More details are in the [Language Spec](https://github.com/kign/c4wa/blob/master/etc/doc/language.md).

## Examples

### Test suite

There is large (and growing) set of tests, from trivial to rather complicated, in 
[this directory](https://github.com/kign/c4wa/tree/master/src/test/resources/c).
For each of these files, you can find generated WAT code [here](https://github.com/kign/c4wa/tree/master/tests/wat).

### Sample Web Application
 
Using compiled WASM file in a Web page is a bit more complicated than simply loading it into `node.js`.

   * For security reasons, browsers can't load WASM from local files (`file:///` protocol);
     you need a local web server to run it.
   * You need `npm` module [browserify](https://browserify.org/) to use any node-targeted code in Web 
      (e.g. [printf](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js)).

There is a sample project in [this directory](https://github.com/kign/c4wa/blob/master/etc/sample_web)
which illustrates how it could be done. Among other features, it also redirects `printf` calls made from C source
to HTML `<textarea>` element.

To try it, simply run `./init.bash` from that directory (it'll check prerequisites, 
install required npm modules, compile the source and load in browser);  
To cleanup, use `./init.bash clean`.

### Game of life

Previously, I had a native WAT implementation for Conway's game of life (on a final toroidal board); 
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

### Game of Life: infinite board

[https://github.com/kign/life-inf](https://github.com/kign/life-inf)

Unlike previous example, this Web Application was designed with `c4wa` in mind. It uses a scalable 
implementation which can support a board of almost any dimensions. Board/Game algorithms are written in C,
and generated Web Assembly file (production version) is about 6Kb. You can also take a look at 
[corresponding WAT file](https://github.com/kign/life-inf/blob/master/etc/bundle.wat).

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
Finally, it will compare binary WASM file generated by `wat2wasm` and one made directly by `c4wa`.

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

### Error testing

Since release 0.4 of the compiler, there are separate _error tests_ 
(see [here](https://github.com/kign/c4wa/tree/master/src/test/resources/errors)) 
consisting of parsable but invalid C code.
`./gradlew test` will verify that each of them will generate expected number of errors and warnings.

