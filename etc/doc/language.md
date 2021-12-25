# Language Spec

`c4wa` compiler operates on a subset of C language. In this section, we attempt to describe this subset.
Obviously, details might change as work on the compiler continues.

## Design goals

  * **Direct translation.**<br> We try to only support features of C language which could be directly and unambiguously
    translated to WAT text format with S-expressions. This way, generated WAT code should be readable and 
   reasonably close to what a human programmer would write.
  * **Cross compilation.**<br> It should be easy to write code which could be compiled and tested 
     with both `c4wa` _and_ an ordinary C compiler. All features which are in some way WASM-specific
    are introduced in a way to make them understood by C compiler as good as possible.
  
## What this compiler is NOT

Just to set expectations correctly, a few things **can't** be guaranteed due to limited scope of this project, 
unavoidable inconsistencies with the standard C compiler, and differences between native executable and 
Web Assembly environment.

  * Your code might successfully pass through `c4wa` but still fail `wat2wasm` compilation. 
    As of version 0.3 of the compiler, there are no known cases of this happening, but it can't be completely 
    ruled out yet.
  * Your code might successfully compile with both regular compiler and `c4wa`, generate correctly working
    native executable, but still work incorrectly in Web Assembly. This could be due to a limited number of known
    inconsistencies you should be aware of when writing code for `c4wa`.
  * There could be occasional instances when due to a bug native executable would fail, yet Web Assembly version
    would still work as expected.
  * `cw4a` might support certain features your C compiler does not, thus your code could compile and correctly execute
    in Web Assembly, yet still require some adaptation to pass through C compiler 
    (for example, `c4wa` doesn't require you to define or declare functions before they are called, as long as they
    are defined later in the code).
  * While `c4wa` is designed to properly report most common syntax error in a way which is broadly consistent with 
    standard C compiler, there is no guarantee it won't successfully compile some obviously invalid C code.
    Most of the testing done on `c4wa`, for obvious reasons, is done on the code which is already known to pass
    throw a C compiler.
  * There is no expectation that any existing C code, other than completely trivial, would pass through `c4wa`
    compilation unchanged. However, for a typical C code, which doesn't rely on external functions
    or libraries (excluding `malloc` and everything you can easily implement or import from your runtime), 
    doesn't use any compiler- or OS-specific features, and doesn't make too much use of the more obscure 
    C language features (`union`s, `goto`s, etc.), making it `c4wa`-compatible shouldn't take too much effort. 

## TL;DR

To get this out of the way as early as possible, 
here are some of the most commonly used features of C language **NOT** supported by `c4wa`:

  * No standard library; other than a handful of built-in utilities, all functions must be implemented or imported
  * `switch`
  * `typedef`
  * `union`
  * `enum`
  * `static` variables or functions
  * `while() ...` loop
  * wide char
  * Pragmas
  * Array initializers
  * Assignment operators `=`, `+=`, `++` etc. in expressions
  * Assignment of `struct`s or using `struct` (not pointer) as an argument
  * `long`/`float` literals
  * labels and `goto`
  * Pointers to arrays, arrays of arrays
  * Function names as variables, indirect function calls
  * Bit Fields
  * Almost all new features introduced in C99 and later standards (except runtime-length arrays, intermingled
    declarations, and one-line comments which are all supported)
  
## A bit more details

As already mentioned, with a few exceptions, `c4wa` out of the box doesn't support any C standard library methods.
Typically, you can import missing functionality from your runtime environment; that's how we could support
a `printf` function, for example. One particular instance where you cannot rely on imported functionality is 
dynamic memory allocation, and `c4wa` does provide certain memory management utilities, covered in detail below. 

`cw4a` supports Web Assembly primitive types (`i32`, `i64`, `f32` and `f64` which translate to C as 
`int`, `long`, `float` and `double` correspondingly), also `char` and `short` 
(which are internally `i32`, except for `struct` members, also some operations work for them differently), 
pointers (including `void *` pointers), structures and arrays. 
Not all possible combinations are supported though, like you can't have
pointer to an array, etc.

Any integer type could be `unsigned`. `sizeof` is supported (but may return results different from native C
compiler due to different pointer size and no alignment in WASM).

`typedef` isn't supported. You must use syntax `struct NAME` when declaring variables of type `struct`.
A `stuct` can have other `struct` as its member ot itself as a pointer. 
Recursive declarations are allowed. There are no `union`s.

`c4wa` supports all C operators, but assignment isn't treated as an operator, so you can't have syntax like
`a = ptr[i ++]` etc. Chain assignments
`a = b = c ...` _are_ supported with some limitations though.

Usual pointer arithmetic is supported; `&` operator can be used with some limitations. 
Note that pointers in `c4wa` are 32 bit.

The ony "native" loop type in Web Assembly is `do ... while()`; you are encouraged to use it whenever practical 
since this creates cleaner and simpler WAT/WASM code. Since it is so common in C, we do 
nevertheless support a regular `for` loop, but not `while() {   }` loop. Use `for(; ... ;)` syntax if you must.
You _can_ use comma `,` to have multiple initializations or increments, though semantic isn't entirely consistent
with C standard.

You can define multiple variables in one definition like `int *a, b, c[2]` and you can 
initialize variables when you define them, e.g.
`int * x = alloc_smth()`, but not both at the same time. There is no initialization syntax for arrays or
structures (except literal strings, which are zero-terminated `char` arrays); neither arrays nor `struct`s 
could be assigned to.

If you reach the end of a non-void function without returning a value, this will trigger 
"unreachable" run time error in WASM even if return value is never actually used.

## Compiling multiple source files

If you specify more than one source files, compiler will yield one "bundle" WAT file.
Functions called from a file they aren't defined in must be declared `extern`; 
more on this below. You can't currently have a global variables shared between multiple
files in a way which would be consistent with C compiler and won't depend on files order, 
so better not try.

## C Preprocessor

`c4wa` relies on external preprocessor installed on your system; by default it is invoked as 
"gcc -E -C", this could be changed with command line option `-Xpreprocessor.command`. 
Preprocessor is not required though, `c4wa` first checks all incoming files for any preprocessor
directives and only runs them through preprocessor when necessary, _or_ if there is any
`-D<name>[=<value>]` command line option.

When preprocessor is used by `c4wa`, symbol `C4WA` is always defined. You can use it to create separate branches
for `c4wa` and standard C compiler.

`c4wa` also comes with a few include files of its own; they are installed as part of ZIP distribution and path is
automatically added to the preprocessor.

Finally, `c4wa` does process so-called "line directives" inserted by a preprocessor and therefore 
will use proper line numbers when reporting syntax errors.

## Built-in functions and built-in libraries

While there is nothing resembling C standard library in `c4wa`, it does support a few utilities. 
They come in the form of _built-in functions_ and _built-in libraries_.

**Built-in functions** are typically such that could be directly mapped to WASM instructions (in other words,
they are _inline_ functions). There is a full list further down in the documentation.

**Built-in libraries**, on the other hand, are separate pieces of functionality that could be optionally
added to the generated WAT. They don't become available unless explicitly "linked" with `-l<library name>`
command line option. Technically, "linking" with such library is functionally equivalent to adding 
additional source files to compile, except these source files are part of the compiler installation.

Note that functions provided by libraries _still need to be declared_ as `extern` before usage; 
built-in functions, on the other hand, do not need to be declared. Some libraries might have dedicated
header files (with a name matching a standard C header file, e.g. `stdarg.h` or `stdlib.h`)

To note, by default with one very small exception (built-in functions `min` and `max` when applied to
integers), `c4wa` does not embed any "library" functionality to generated WAT; you would only get 
whatever functions are in your source and nothing else. On the other hand, incorporating a built-in library, 
for example for memory management, could add noticeable amount of additional library code.

## Import and export

Syntax of C doesn't exactly match Web Assembly concepts of "imported" and "exported" symbols (global variables, functions
and memory); instead of introducing new incompatible syntax, 
`c4wa` solves this problem by reinterpreting existing attributes, as follows: 

**Function definition** could be `extern`; this makes function _exported_. 
A function which is not `extern` will not be exported. Obviously, you should always have at least one `extern` function,
but you can have as many as you want.

`c4wa` will only output WAT source for functions which are exported (that is, declared `extern`) or are called from an
exported function. If you have no exported functions, you'll get an empty module and a warning.

**Function declaration** could be `static` or `extern`; either attribute will cause declared function _not_ 
to be imported. If neither attribute is present, it will be considered imported. 

For example, then you declare function like `double atan2(double, double)` (no attributes), 
it is interpreted as _imported_, and
if not provided by the run-time, this will trigger a error. 

`static` and `extern` declarations are for functions defined elsewhere in the same file (in case of `static`)
or in another file (`extern`). You never need `static` declaration to compile with `c4wa` , but you might
need it for compatibility ith standard C compiler. `extern` declaration could come handy if you have more
than one source file to compile, or when using a built-in library.

Be careful: an attempt to declare function without `extern`, while perfectly legal in standard C,
will lead  `c4wa` to treat your function as imported, and if it is defined later (including in a library), 
it'll trigger a compiler error.

```c
double atan2(double, double); // no attribute, function considered imported

..................
double x = atan2(2.0, 3.0);   // no problems so far, function is declaraed

..................
double atan2(double y, double x) {  // oops, can't define imported function, compiler error;
                                    // change declaration to add `static` or `extern`
```

**Global variables** could be `static`, `extern`, or neither. `extern` variable will be _exported_, and neither `extern`
nor `static` will be _imported_ (just like a function declaration). `static` global variables are neither 
imported not exported.

(Global variable could also be `const`, it which case it is implicitly `static` unless declared as `extern`).

**Memory** behaviour is determined by compiler options 
(see [here](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md)). It could be imported, 
exported (current default), purely internal or not be present at all.

All objects are exported and imported under their actual names in C (except memory, which doesn't have a C
identifier and so export/import name is determined by compiler option `module.memoryStatus`). 
When importing, module name is set by compiler option `module.importName` (default is `c4wa`). So for example,
if you want to import function `atan2` from JavaScript runtime, you declare it in C source as
`double atan2(double, double)` (remember: no attributes), and then use this code in JavaScript to import:

```javascript
WebAssembly.instantiate(wasm_bytes, {c4wa: {atan2: Math.atan2}});
```

Any C attribute not listed above is explicitly not allowed 
(so for example you can't have `static` function definition).

## Memory

A simple C program might not require a linear memory at all; you can use 
[compiler option](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md) `module.memoryStatus=none`
to not add any memory declaration to generated WAT file. However, many features, such as 
taking address of a local variable, using `struct`s or arrays, 
calling functions with variable number of arguments (like `printf`), 
and obviously allocating memory directly, won't work without 
linear memory.

### Composition of linear memory

![Linear memory](https://github.com/kign/c4wa/blob/master/etc/doc/memory.png?raw=true "Linear memory" )

Generated WAT will have two special blocks of linear memory with configurable sizes.

  * **Stack**, size `module.stackSize` (default 1024), for stack variables;
  * **Data**, for string literals; size is flexible depending on actual space used.

Note that very first byte of memory (address 0) isn't used by the stack; this is done so no pointer 
could have value 0.

The rest of memory, from byte number  `module.stackSize` + (actual data size) onwards,
can only be accessed by using `__builtin_memory` variable or one of provided memory managers.

### Low-level memory access

You can access linear memory directly from your C program by utilizing built-in global variable `__builtin_memory`.
(It has type `void *` and actual numeric value `0`).
This has to be done very carefully, obviously, because you must make sure you assign memory
correctly, and because you may override stack section or data section.

For that reason, `__builtin_memory` should always be used together with another 
built-in global variable `__builtin_offset` which is set to the offset where data segment ends. Its
type is `int`.

### Memory managers

Memory manager is a module built on the top of low-level access to  `__builtin_memory` 
to implement methods like `malloc` and `free` for dynamic memory access.

There are currently three memory managers, in order of increasing complexity:

| Library name | Description                                             |
|--------------|---------------------------------------------------------|
| `mm_incr`    | Incremental memory allocation; nothing is ever released |
| `mm_fixed`   | Fixed-sized chunk allocation                            |
| `mm_uni`     | Universal memory manager                                |

Incorporating universal memory manager with command line option `-lmm_uni` pretty much allows a programmer to use 
`malloc` and `free` as one normally would. In many ways, this is not the most optimized solution though,
and it could be an overkill for simpler tasks. 

### stack variables

Web Assembly supports unlimited number of local variables, so when you have a local variable in C,
we directly map it to a local variable in Web Assembly 
(see [below](https://github.com/kign/c4wa/blob/master/etc/doc/language.md#local-variables-mapping) 
on how WAT names for these variables are selected). 

In some situations though we have to store value in the _stack_, keeping its _address_ as a local variable.
In this case we refer to this variable as _stack variable_. This happens in these two cases:

First, this happens if you declare a variable of type array (not pointer) or `struct`. In this case,
your variable is allocated in the _stack_ (first `module.stackSize` bytes of linear memory). Actual WASM local variable
holds a _pointer_ to this memory.

### `&`

However, `cw4a` would also assign a regular primitive type variable to the stack _if you attempt to take an
address of this variable_. 

Consider this C code:

```c
void foo(int par) { ... }

void bar() {
    int a = 14;
    foo(a);
}
```

Compiled to WAT, function `bar` will look like this:

```wat
(func $bar
  (local $a i32)
  (set_local $a (i32.const 14)) 
  (call $foo (get_local $a)))
```

Let's now change parameter of `foo` to a pointer and argument to `&a`:

```c
void foo(int * par) { ... }

void bar() {
    int a = 14;
    foo(&a);
}
```

This simple change will result in a very different WAT code for `bar :

```wat
(func $bar
  (local $@stack_entry i32)
  (local $a i32)
  (set_local $@stack_entry (global.get $@stack))
  (set_local $a (global.get $@stack))
  (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
  (i32.store (get_local $a) (i32.const 14))
  (call $foo (get_local $a))
  (global.set $@stack (get_local $@stack_entry)))
```

What happened here? We can't pass an address of a local variable; the only way to return a value from a 
Web Assembly function other than through a return value is through linear memory: to pass a memory address
(or index) and have function write data to this address.

So, there is still a local variable `$a` but now it holds a memory address. Any attempt to access it
will necessitate memory access. Additionally, we need to adjust stack pointer, preserve stack pointer value at function
entrance and restore it at all function exit points.

Still, If everything works as it should, whether a local variable is allocated in the stack
or not should make no difference in execution, but performance might well suffer. 

You can't take an address of an array (since it's already the address) or a global variable 
(since they can't be put on the stack)

One peculiarity of `c4wa` is that expression `&a[1]` is interpreted as `(&a)[1]` and not `&(a[1])` as it should.
This is related to left recursion in Antlr4, and I haven't been able to solve this yet without significant
changes to the grammar. For practical use, this is hardly a problem, you can always use parenthesis or
simply replace this expression with `a + 1`, which is what it is anyway.

### stack arrays

You can bypass manual memory allocation by using stack. Variable-length arrays are supported:
When you declare an array, `type variable[size]`, `size` doesn't have to be a compile-time constant, 
it could be any valid integer expression, subject to the limit imposed by allocated stack space.

For example, if you need to allocate integer array of size `N` and fill it in with consecutive numbers `0 ... N-1`,
either of these two alternatives will work:

```c
extern void * malloc(int);

int * arr = malloc(N * sizeof(int)); // make sure to link with a suppored memory manager!
for (int i = 0; i < N; i ++
    arr[i] = i; 
```

or

```c
int arr[N];
for (int i = 0; i < N; i ++)
    arr[i] = i; 
```

in the 2<sup>nd</sup> version you don't need to worry about selecting or implementing a memory manager; 
however, you need to be mindful of available stack size (see above);
we are _not_ checking for stack overflow, so if you take too much memory you'd start overwriting your DATA section. 

Arrays are permitted in `struct`s, but must have fixed (= known at compile time) size. 

### Use case: returning complex data types from exported functions

While `c4wa` compiler allows us to write a code where C functions could exchange complex data structures
via linear memory, it can't provide a ready-to-use solution if there is a need to exchange such data
types between Web Assembly code and the runtime, since it knows nothing of the runtime.

Consider one example. Let's say we need to call a function from JS runtime to return a boundary
box for a certain 2D region, as determined by 4 numbers: `xmin`, `xmax`, `ymin`, `ymax`.

A regular C function would look like this:

```c
void find_boundary_box(int * p_xmin, int * p_xmax, int * p_ymin, int * p_ymax) { ... }  
```

We can't easily use it as an exported function though, because then the runtime would need to know exactly which memory
addresses it could pass as parameters, and this would mean that memory allocation inside C/WASM code would 
need to be coordinated with the runtime. It's not undoable, but it's complicated and a bad design.

One obvious alternative is to simply split this into 4 separate function for each integer to be returned,
with some internal cache to avoid unnecessary repeated calculations. This has an advantage of being limited
to C/WASM code and not requiring any new logic in terms of communications with the runtime; but it's still
complicated, requires a separate logic to save and invalidate static cache, etc.

Finally, we could allocate new memory region, store 4 integers there, and return memory address to the runtime.
This is a lot better, but one remaining issue is that if we want to reclaim this memory later, it'll again
require a rather complicated logic since it couldn't be released in a function where it was allocated.

Using stack allocation solves this last problem, because stack memory is already tracked globally.
So here is what we can do:

C-code:

```c
extern int * find_boundary_box() {
    int boundary_box[4];
    ......................
    boundary_box[0] = xmin;
    boundary_box[1] = xmax;
    boundary_box[2] = ymin;
    boundary_box[3] = ymax;
    
    return boundary_box;
}
```

JavaScript runtime:

```javascript
// ............................
const wasm = await WebAssembly.instantiate(bytes, import_object);
const exports = wasm.instance.exports;
const linear_memory = new Uint8Array(exports.memory.buffer);
const boundary_box = exports.find_boundary_box();
const [xmin, xmax, ymin, ymax] = [...Array(4).keys()].map(i => read_i32(linear_memory, boundary_box + 4 * i));
```

Function `read_i32` could be imported from 
[here](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js).

If you are verifying your code in standard C compiler (which is recommended), it will probably complain 
about returning stack value from a function `find_boundary_box`. You can restructure your code slightly to
avoid this warning:

```c
extern int * find_boundary_box() {
    int boundary_box[4];
    int * ret_val = boundary_box;
    ......................
    boundary_box[0] = xmin;
    boundary_box[1] = xmax;
    boundary_box[2] = ymin;
    boundary_box[3] = ymax;
    
    return ret_val;
}
```

### Memory functions

These functions behave as normal C function in C code with given signatures 
but `c4wa` internally replaces them with Web Assembly memory operators:

| Name    | Arguments                               | Return Value | Description                                                      | 
|---------|-----------------------------------------|--------------|------------------------------------------------------------------|
| memset  | `void * addr`, `char value`, `int size` | _void_       | Same as in C library                                             |
| memcpy  | `void * dest`, `void * src`, `int size` | _void_       | Same as in C library                                             |
| memgrow | `int n_pages`                           | _void_       | Increase memory size by specified number of pages (1 page = 64K) |
| memsize | _none_                                  | `int`        | Get current memory size in pages                                 |

Note that if using `memset` and `memcpy`, you'll need option `--enable-bulk-memory` to use `wat2wasm`.
Also, generated WASM module might not be compatible with some runtimes (such as `wasmer` as of this writing).

Since `memgrow` and `memsize` are WASM-specific, when cross-compiling with a native C compiler
you should provide a suitable replacement, e.g.

```c
#ifndef C4WA
static int __memory_size = 1;
#define memgrow(size) __memory_size += (size)
#define memsize() __memory_size
#endif 
```

Note also that while [memory.grow](https://github.com/sunfishcode/wasm-reference-manual/blob/master/WebAssembly.md#grow-linear-memory-size)
instruction in WASM does return a value (previous memory size on success, -1 on failure), it is currently ignored
(dropped) in `c4wa`.

## Built-in functions

In addition to memory functions `memset`, `memcpy`, `memgrow`, `memsize` discussed above, 
there are a few other built-in functions:

  * `min` and `max` work with any numerical arguments (of the same type), and will return result of the same type as arguments;
  * `floor`, `ceil`, `sqrt`, `fabs`. These functions work for `float` or `double` arguments, and will return same type as passed;
  * `abort` triggers "_RuntimeError: unreachable_" exception;
  * `__builtin_clz`, `__builtin_ctz`, `__builtin_clzl`, `__builtin_ctzl`, `__builtin_popcount`, `__builtin_popcountl` 
    (see gcc [documentation](https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html)). 
    Note that while
    in gcc behaviour of CLZ/CTZ functions is explicitly undefined if argument is 0 
    (in practice, implementations typically return 0), 
    in WASM these functions return full number of bits in the argument (so 32 for first two, 64 for the last). Note also
    that in GNU C compiler, builtin functions don't need to be declared, thus you don't need any extra glue to
    cross-compile with a GNU-compatible compiler (just be mindful of argument 0).

## Strings and chars

Web Assembly has special DATA section and `data` instruction to store strings in memory. 
Memory for DATA is allocated at compile time based on actual total lengths of all string literals (plus 
terminating zero byte).  

All string literals in C code are placed in DATA section with terminating `\0`; 
identical strings are assigned same memory address.
When assigned to a variable or passed as an argument to a function, string literals have type `char *`.

Consecutive string literals are joined together, so the following code is legal:

```c
char * file = 
    "Line 1\n"
    "Line 2\n"
    "Line 3\n";
```

Unlike most C implementations, _string literals are writable_. The following code will work
in Web Assembly but will probably trigger a _Bus Error_ with native C compiler:

```c
char * name = "peter";
name[0] = 'P';
```

Char literals are supported, including all standard escape sequences.

Note that strings and `char`s are 8-bit. If you include a Unicode character in a string, it'll be decoded 
into bytes with UTF-8 encoding. You can't have a Unicode character as `char` literal.

You can use built-in function `memcpy` to copy string literal to `char` array, but remember to account for 
terminating zero byte:

```c
char name[5];
memcpy(name, "John", 5);
```

For example, this is a `c4wa`-compatible implementation of `strlen` function:

```c
int strlen(char * str) {
    int n = 0;
    do {
        str ++;
        n ++;
    }
    while(*str);
    return n;
}
```

You can freely pass strings to imported functions, which will then need to read actual characters from memory
(and probably convert 8-bit data to Unicode strings); this is how `printf` function works in the testing suite.

It must be acknowledged that `c4wa` isn't a good environment to write a code dealing with strings. 
This is in part because C itself isn't, and in part because working with strings means often 
allocation and freeing up memory, and you do need a decent memory manager for that.

### Functions with variable number of arguments

`c4wa` fully supports standard C syntax to define or declare functions with variable argument list.
The following example (borrowed from [here](https://www.cprogramming.com/tutorial/c/lesson17.html);
note that on this occasion, original C code compiles in `c4wa` without a single change) 
will compile and produce same output in both `c4wa` and standard C:

```c
#include <stdio.h>
#include <stdarg.h>

double average (int num, ...) {
    va_list arguments;
    double sum = 0;
    va_start ( arguments, num );
    for ( int x = 0; x < num; x++ )
        sum += va_arg ( arguments, double );
    va_end ( arguments );
    return sum / num;
}

extern int main() {
    printf( "%.2f\n", average ( 3, 12.2, 22.3, 4.5 ) );
    printf( "%.2f\n", average ( 5, 3.3, 2.2, 1.1, 5.5, 3.3 ) );
    return 0;
}
```

You can also have imported functions with variable arguments; when adding them to your runtime,
actual implementation will have one additional argument after required ones, which is a memory
address where all subsequent arguments shall be read (it'll be passed even there are no
additional arguments in the function call). Each optional argument, regardless of type,
will occupy exactly 8 bytes (64 bits) in linear memory.

## `printf`

The best example of this approach is function `printf` from the test suite.

For the purposes of `c4wa`, it is defined as follows:

```c
void printf(char * format, ...);
```

(You can also include file `stdio.h`, which as of current version doesn't have anything except this one line).

Since there are no attributes, this is an _imported_ function; since there is exactly one required argument,
actual runtime implementation would have _two_ arguments, `format` and `offset`.

Let's consider this call of `printf` :

```c
int A;
unsigned long B;
double C;
char * D = "some string";
........................
printf("A = %d, B = %lx, C = %.6f, D = %s\n", A, B, C, D);
```

In this case, there are 5 _actual_ arguments, but imported function will still be called with _two_ arguments:

1-st argument `format`: memory address to read format string from 
(just like in C, any array, including string, is passed as memory address of its first element);<br>
2-nd argument `offset`: memory address to read the read of arguments from.

To acquire actual values `A`, `B`,`C` and `D`, implementation will then need to gain access to linear memory
and read arguments at the following memory locations:

Variable `A` at address `offset` (actual 32-bit value of `A` is converted to 64–bit);<br>
Variable `B` at address `offset + 8`;<br>
Variable `C` at address `offset + 16`;<br>
String `D` is a string to be read from a location which value (32-bit converted to 64) is stored at address `offset + 24`.

When passing arguments, all integer values are converted to `long`, and all float values to `double`.

There is a sample `node.js` runtime implementation of `printf` 
[here](https://github.com/kign/c4wa/blob/master/etc/wasm-printf.js), which you can re-use. 
It doesn't archive 100% compatibility with C standard, but it is reasonably close.
File [run-wasm](https://github.com/kign/c4wa/blob/master/etc/run-wasm) 
is an example of how it could be used in a runtime if WASM code is exporting memory.

## Operators

`c4wa` supports all 40+ C operators, with only minimal and mostly inconsequential differences with standard C. 
Known inconsistencies and bugs are:

  * Incorrect prioritization of `&`;
  * Assignment operators: `=`, `++`, `--`, `+=`, `-=`, `*=`, `/=`, `%=`, `>>=`, `<<=`, `&=`, `^=`, `|=` could 
    not be re-used in an expression; operators in `c4wa` have no immediate side effects (that is, other than via
    function calls). Thus, operators `++` and `--` are postfix only (`a ++` is valid, `++ a` is not);
  * Comma `,` isn't technically an operator, it's an alternative to block `{ ... }` to make a composite statement.
   `a = b, c` is illegal, but `a = b, c = d` or `i ++, j ++` are ok;
  * Boolean expressions `!!x`, `!(x == 0)`, `!x == 0`, `x != 0`, `(x == 0) == 0` are always simplified to just `x`, 
   whereas it should be 1 if `x` ≠ 0.

### Boolean operators and values

Booleans should be reasonably consistent with C: `0` is _false_, `1` is _true_, etc. There isn't any built-in
support for `true` or `false` constants, feel free to add your own via preprocessor or globals.

One thing to note, `c4wa` _does_ support proper semantics for boolean `&&` and `||` (so when evaluating
`A && B`, if `A` evaluates to _false_, `B` is not evaluated, similarly for `A || B`), 
but at a price of generating more complex WAT code 
(since there is no built-in support for such operations in Web Assembly). You may consider
using bitwise `&` and `|` instead in some situations, which directly translate to WASM instructions
resulting in simpler and faster code.

## Casts and constants

The rules of automatic casting in `c4wa` are broadly consistent with a standard C compiler, 
but perhaps somewhat simplified. 

If an assignment, function call, `return` expression or binary operation is used with inconsistent
types, `c4wa` will automatically and silently apply a cast to bring lower-width value to higher-width,
and will also convert any integer type to any float type regardless of width (so assigning `long` to `float` is OK), 
but _not_ the other way around without an explicit cast.

```c
int a;
long b;
float f;
double g;

b = a; // OK
a = b; // Syntax error
g = f; // OK
f = (float) g; // woudn't work without a cast 
```

(additionally, when integer types involved are only different by only one of them being `unsigned`, 
this will trigger compilation error without an explicit cast).

Now, all integer constants in `c4wa` have type `int` and all float constant have type `double`. This may
create a problem when initializing a `float`, for example

```c
float x = 1.14;
```

Is this legal? `1.14` is a `double` which can't be assigned to a `float` without a cast. `c4wa` solves this
problem by applying a special rule to constants: they automatically adopt the type of the non-constant operand.
So, when you assign `1.14` to a `float`, it automatically becomes `float`, as if you wrote 

```c
float x = (float) 1.14;
```

A few other examples or permissible and not permissible assignments:

```c
long longNumber = -18;
float floatNumber = 1.234e2;
int intNumber = -57.4; // not even a warning, unlike standard C compiler

int * ptr = 0; // still OK

long * lptr = 1;   // Nope, this is explicitly not allowed. Only constant `0` could be assigned to a pointer.
```

Sometimes these two rules could be in conflict. For example, how shall we interpret comparison `x > 1.0`, 
where `x` is a `float` value? We could either interpret `1.0` as float and perform 32-bit float comparison,
or convert `x` to `double` to do 64-bit float comparison.

Current implementation tends to prefer the latter, but this is still work in progress and subject to change.

To assign one pointer to another, they must have the same type, unless one of them is `void *`.

## `NULL`

While there is no built-in `NULL` constant, you can define `NULL` as `0` and all customary C syntax would
work as expected:

```c
#define NULL 0

.................
int * p_x = NULL;

if (p_x) { ....

if (!p_x) { ....

if (p_x && *p_x > 0) { ....

```

The only caveat is that 0 pointer value _could_ be dereferenced. The following code won't trigger any run-time errors
but will silently overwrite some of your stack space:

```c
int * p_int = 0;
* p_int = 57;
```

(In theory, we might have used `-1` instead of `0` as an illegal pointer value, so an attempt to dereference it would
have failed; this however would lead to quite a lot of complications, like properly interpreting pointers as booleans,
`if (ptr) {...}`, etc; it is way easier to keep NULL=0. While `NULL` checks by themselves are mostly OK as a 
design patter, a programmer shouldn't rely too much on runtime errors as validation. Also, in the future
we may add a special "debug" mode with some additional run-time checks, including stack overflow and dereferencing 0).

## Local variables mapping

Since WAT format supports local variables, it is tempting to simply map local variables in C directly to WAT
names, so that if for example you have variable `long acnt_id` in C code, it'd be mapped to `(local $acnt_id i64)`
in WAT, as that's exactly what `c4wa` does, most of the time. However, since release 0.4, `c4wa` supports
block scope for local variables, and it makes things tad more complicated.

Consider this fragment of C code:

```c
int a;

for (...) {
   double a = ...;
.....................   
```

Now we have two variables with names `a`, in exterior scope and inside the block; they even have different types,
and so we must choose two separate WAT names for them. In this case, `c4wa` maps first `a` to `$a`, and
all variables `a` inside the embedded blocks to some auto-generated names based off original variable name `a`
and unique block id.

Now, let's consider the subsequent code in the same fragment:

```c
int a;

for (...) {
   double a = ...;
.....................
}

double x = -57;   
```

When we get to variable `x`, we _could_ map it directly to `$x` in WAT code; however, this would not be 
the most optimal solution, since we already had to add a `double`=`f64` variable for interior `a` ;
thus `c4wa` always attempts to re-use no longer needed (out of scope) variables, if type matches.

At the end, mapping to WAT names could get complicated; the end result however is WAT code which fully
respects block scoping for variables and uses only minimal necessary number of `local`'s. 

## Globals

Web Assembly supports global variables, so you can freely use them in C code. However, Web Assembly requires that
all non-imported globals be initialized, and furthermore, you can only initialize globals to compile-time
constants.

```c
int Num_of_Points;        // imported, can't initialize
extern double Volume = 0; // exported, must initialize
static N = 10;            // internal, neither imported or exported
const test_mode = 1;      // non-mutable, implicitly 'static' unless declared 'extern'
```

Global values could be initialized to any compile-time constant; compile-time expressions may use `sizeof`.

## `const`

Unlike standard C, `const` attribute isn't part of type definition, but optional attribute of variable
initialization (remember what in `c4wa` you can initialize only one variable, `int a = 1, b = 1` isn't valid).

```c
const int x = N + 1;         // OK, `x` can no longer be assigned to
const int x;                 // invalid
void main(const char * []);  // invalid 
```

## Cross-compiling with C

Cross-compiling the code with standard C compiler is listed above as one of the design goals, 
and it should indeed be simple and straightforward; `c4wa` does not use any special syntax or markers 
not known to a C compiler. Nevertheless, a few adjustments might be necessary:

  * Due to incompatible dynamic memory semantics, 
    you need to either emulate customary C functions `malloc` and `free` in `c4wa` 
   (e.g. by using one of the provided memory managers) , 
    or emulate linear memory in native C compiler; 
  * Some built-in functions might not be available in C library or require an explicit declaration;
  * `cw4a` is tolerant to functions calling each other in any order within the same file.

In addition to including proper header files for C library functions (such as `malloc`, `free` or `printf`), 
you would need to somehow emulate WASM-specific functions which do not exist in standard C library. 
This is an example of the header you can include into your program:

```c
#ifndef C4WA
static int __memory_size = 1;
#define memgrow(size) __memory_size += (size)
#define memsize() __memory_size
#define min(a,b) ((a) < (b))?(a):(b)
#define max(a,b) ((a) < (b))?(b):(a)
#endif
```

## Comments

Both C-style `/* ... */` and C++ line comments `// ........` are supported.
