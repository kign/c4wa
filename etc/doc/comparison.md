# `c4wa` vs other compilers

Let's take a simple C function we already used in the [overview](https://github.com/kign/c4wa/blob/master/README.md#usage)
and try to compile it to Web Assembly using some known compilers.

**emscripten**

`Emscripten` is closest, since it already compiles `C` code.
To compile with [emscripten](https://developer.mozilla.org/en-US/docs/WebAssembly/C_to_wasm), we need to first change
our function slightly (so that `emscripten` would include it in the generated Web Assembly):

```c
#include <emscripten/emscripten.h>

EMSCRIPTEN_KEEPALIVE int collatz(int N) {
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

Emscripten creates relatively small WASM file (under 1K) and ...  124K (2300 lines) accompanying 
JavaScript file "containing glue code to translate between the native C functions, and JavaScript/wasm".

Regardless, let's try to look a the generated code for out function. That's not trivial, since we only have 
WASM, not WAT file. We can use tool `wasm-decompile` which translates WASM to some semblance of C code. Here is
out collatz() function:

```c
export function collatz(a:int):int {
  var b:int = g_a;
  var c:int = 16;
  var d:int_ptr = b - c;
  d[3] = a;
  var e:int = d[3];
  d[1] = e;
  loop L_b {
    var f:int = d[1];
    var g:int = 1;
    var h:int = f;
    var i:int = g;
    var j:int = h == i;
    var k:int = 1;
    var l:int = j & k;
    if (eqz(l)) goto B_c;
    goto B_a;
    label B_c:
    var m:int = d[1];
    var n:int = 1;
    var o:int = m & n;
    if (o) goto B_e;
    var p:int = d[1];
    var q:int = 1;
    var r:int = p >> q;
    d[1] = r;
    goto B_d;
    label B_e:
    var s:int = d[1];
    var t:int = 3;
    var u:int = s * t;
    var v:int = 1;
    var w:int = u + v;
    d[1] = w;
    label B_d:
    var x:int = d[2];
    var y:int = 1;
    var z:int = x + y;
    d[2] = z;
    var aa:int = 1;
    var ba:int = 1;
    var ca:int = aa & ba;
    if (ca) continue L_b;
  }
  label B_a:
  var da:int = d[2];
  return da;
}
```

It's really, really difficult to recognize out original logic here. Among other things, whereas our
original C code only has 2 local variables, this version has 29, not counting module-level globals.

Contrast it with the result of applying `wasm-decompile` to WASM file generated from `c4wa` compiler:

```c
export function collatz(a:int):int {
  var b:int;
  var c:long = i64_extend_i32_s(a);
  loop L_b {
    if (c == 1L) goto B_a;
    if (eqz(c % 2L)) { c = c / 2L } else { c = 3L * c + 1L }
    b = b + 1;
    continue L_b;
  }
  label B_a:
  return b;
}
```

Pretty much same logic we had originally.

On top of that, to run this WASM file one only needs 3 lines of JavaScript code (or any other Web Assembly runtime), to
read the file, to instantiate `WebAssembly` object, and to call exported function. Nothing else.

**zig**

As another experiment, consider [Zig language](https://ziglang.org/), which comes prepackages with Web Assembly compiler.
First, we re-write our function in `Zig`:

```zig
export fn collatz(N: i32) i32 {
    var len: i32 = 0;
    var n: u64 = @intCast(u64, N);
    while (true) block_label: {
        if (n == 1)
            break :block_label;
        if (n % 2 == 0) {
            n /= 2;
        } else {
            n = 3 * n + 1;
        }
        len = len + 1;
    }
    return len;
}
```

We can compile to Web Assembly like this:

```bash
zig build-lib collatz.zig -target wasm32-freestanding -dynamic
```

Unlike `emscripten`, there is no "glue" JavaScript; generated WASM could be immediately loaded. However, this WASM
file is already 52K (as a reminder, `c4wa` fits this function into 99 bytes). Using again `wasm-decompile`
to extract code for this specific function, it appears that it is even more complicated than `emscripten` version.

None of that is necessarily a deal breaker. The huge advantage of using exising compilers is that
they fully implement each and every feature of the language, and often significant part of standard library. 
For example, [Assembly Script](https://www.assemblyscript.org/) compiler (a higher level language specifically 
created for Web Assembly, as a sibling of TypeScript) embeds into generated WebAssembly a garbage collector.
This allows a developer to compile into Web Assembly with few, if any, changes, almost any existing code
in a supported language.

When writing a new code, however, full feature support is less important. What you want is to generate
reasonably efficient Web Assembly code, and to do in a reasonably high-level language.

Why does it have to be a subset of C though?

There are several projects to introduce a wrapper around WAT to make it easier to work with
(for example, [wah](https://github.com/tmcw/wah)). While not a bad approach, *an advantage of using 
a subset of C is to be able to compile and test the code also in C*.

Let's again go back to the original file `collatz.c`.

```c
extern int collatz(int N) {
    int len = 0;
    unsigned long n = (unsigned long) N;
    do {
        if (n == 1)
            break;
        else if (n % 2 == 0)
            n /= 2;
        else
            n = 3 * n + 1;
        len ++;
    }
    while(1);
    return len;
}
```

We could easily add to it `main` function

```c
int printf ();
int atoi ();

int main(int argc, char * argv[]) {
    int n = atoi(argv[1]);
    printf("Cycle length of %d is %d\n", n, collatz (n));
}
```

Compile, and verify the result:

```bash
gcc -Wno-incompatible-library-redeclaration main-collatz.c -o collatz
./collatz 626331
# Cycle length of 626331 is 508
```

In addition to testing, using C language automatically gives us C preprocessor, a useful tool for inlining or
conditional compilation. 

(Of course, in theory, nothing could stop us from using C preprocessor,
or any other macro processor language for that matter (like the most popular one `m4`) on top of
another language, or even on top of plain WAT. The problem with that approach though is that we are 
de-facto creating a new language (e.g. WAT + m4) which
would have no syntax or other support in any existing IDE or other common tools. By contrast, C preprocessor
would fit perfectly with C language).

