# Language Spec

`c4wa` compiler operates on a subset of C language. In this section, we attempt to describe this subset.
Obviously, details might change as work on the compiler continues.

## Design goals

  * **Direct translation.**<br> We try to only support features of C language which could be directly and unambiguously
    translated to WAT text format with S-expressions. This way, generated WAT code should be readable and reasonably close to what 
    a human programmer would write.
  * **Functionality first, syntax sugar later.**<br> Implement the widest possible scope of C language features first, worry 
    about convenience only as necessary.
  * **Cross compilation.**<br> It should be easy to write code which could be compiled and tested 
     with both `c4wm` _and_ an ordinary C compiler. All features which are in some way WASM-specific
    are introduced in a way to make them understood by C compiler as good as possible.
  * **Minor Incompatibilities**<br> On the other hand, since there is no goal to provide full implementation
    of C standard, we are not worried about minor or inconsequential incompatibilities which have little impact
    on everyday programming. All known incompatibilities are documented below.

## What this compiler is NOT

Just to set expectations correctly, a few things **can't** be guaranteed due to limited scope of this project, 
unavoidable inconsistencies with the standard C compiler, and differences between native executable and 
Web Assembly environment.

  * Your code might successfully pass through `c4wa` but still fail `wat2wasm` compilation. 
    The plan in to eventually try to verify generated code as much as possible to avoid incorrect WAT output, 
    but it's still very much work in progress. 
    Fortunately, since generated WAT code could be easily traced to the original source in C, 
    such errors are easy to fix in C code (unless it is a compiler bug).
  * Your code might successfully compile with both regular compiler and `c4wa`, generate correctly working
    native executable, but still work incorrectly in Web Assembly. This could be due to a limited number of known
    inconsistencies you should be aware of when writing code for `c4wa`.
  * There could be occasional instances when due to a bug native executable would fail, yet Web Assembly version
    would still work as expected.
  * `cw4a` might support certain features your C compiler does not, thus your code could compile and correctly execute
    in Web Assembly, yet still require some adaptation to pass through C compiler 
    (for example, `c4wa` doesn't require you to define or declare functions before they are called, as long as they
    are defined later in the code).
  * There is no expectation that any existing C code, other than completely trivial, would pass through `c4wa`
    with no changes. Moreover, we are not making too much effort to make adaptation to `c4wa` easier,
    since it would be mostly pointless. However, for any normal C code, which doesn't rely on external functions
    or libraries, doesn't use any compiler- or OS-specific features, and doesn't make too much use of the more obscure 
    language features (`union`s, `goto`s, etc.), making it `c4wa`-compatible shouldn't take too much effort. 

## TL;DR

Here are some of the most commonly used features of C language **NOT** supported by `c4wa`:

  * No standard library; other than a handful of built-in utilities, all functions must be implemented or imported
  * `switch`
  * `typedef`
  * `union`
  * `enum`
  * `static` variables or functions
  * `while() ...` loop
  * wide chars
  * `void *` pointers
  * Pragmas
  * Array initializers
  * Assignment operators `=`, `+=`, `++` etc. in expressions
  * Assignment of `struct`s or using `struct` (not pointer) as an argument
  * `long`/`float` literals
  * labels and `goto`
  * block scope for local variables
  * Pointers to arrays, arrays of arrays
  * Function names as variables, indirect function calls
  * Bit Fields
  
## A bit more details

_We list below most important known limitations of `c4wa` compiler, inconsistencies with standard C, or specifics of WASM
runtime, in no particular order. Some of these we'll cover in more details further down._ 

Web Assembly doesn't have any memory management features, instead giving programmers access to one single memory block 
("linear memory"). Correspondingly, `c4wa` has only limited dynamic memory allocation capabilities; for anything beyond
that, developer must assign free space to all dynamic objects manually (more on that below).

`cw4a` supports Web Assembly primitive types (`i32`, `i64`, `f32` and `f64` which translate to C as 
`int`, `long`, `float` and `double` correspondingly), also `char` and `short` 
(which are internally `i32`, except for `struct` members, also some operations work for them differently), 
pointers, structures and arrays. Not all possible combinations are supported though, like you can't have
pointer to an array, etc.

Any integer type could be `unsigned`. `sizeof` is supported (but may return results different from native C
compiler due to different pointer size and no alignment in WASM).

`void` keyword is recognized, but it isn't really a type, but more like indicator of "no type". 
For now, `void` could only be used in function
definition or declaration to indicate "no return value". You can't have `void *`, etc.

`typedef` isn't supported. You must use syntax `struct NAME` when declaring variables of type `struct`.
Recursive declarations are allowed. There are no `union`s.

`c4wa` supports all C operators, but assignment isn't treated as an operator, so you can't have syntax like
`a = ptr[i ++]` etc. Chain assignments
`a = b = c ...` _are_ supported with some limitations though.

Usual pointer arithmetic is supported; `&` operator can be used with some limitations.

There are no `void *` pointers. 
If you are using a "generic" pointer, you must explicitly cast it.
Built-in functions like `memcpy` use `char *`.  

There is no built-in `NULL`. You can of course compare your pointer with `(type *) 0` if you wish, but technically
0 is a valid pointer value (this will be the address of your first local variable
allocated on the top of the stack, which starts from memory address 0), so be very careful when 
doing something like that; it's ok to use pointers in conditionals `if (ptr} { *ptr = ... ` if you do want 
to compare with 0.

Almost all arithmetic operations, function calls and assignment require explicit type cast if types are different.

Local variables can be introduced anywhere in the program, but all share scope of the function. There aren't 
block-level locals.

The ony "native" loop type in Web Assembly is `do ... while()`; you are encouraged to use it whenever practical 
since this creates cleaner and simpler WAT/WASM code. Since it is so common C, we do 
nevertheless support a regular `for` loop, but not `while() {   }` loop. Use `for(; ... ;)` syntax if you must.
You can use comma `,` to have multiple initializations or increments.

You can define multiple variables in one definition like `int *a, b, c[2]` and you can 
initialize variables when you define them, e.g.
`int * x = alloc_smth()`, but not both at the same time. There is no initialization syntax for arrays or
structures (except literal strings, which are zero-terminated `char` arrays); neither arrays nor `struct`s 
could be assigned to.

If you reach the end of a non-void function without returning a value, this will trigger "RuntimeError: unreachable"
in WASM even if return value is never actually used.

Function declarations (unlike definitions) can't have parameter names, only types.

## Compiling multiple source files

If you specify more than one source files, compiler will yield one "bundle" WAT file.
Functions called from a file they aren't defined in must be declared `extern`; 
more on this below. You can't currently have a global variables shared between multiple
files in a way which would be consistent with C compiler, so better don't try.

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

`static` and `extern` declarations are for functions defined elswhere in the same file (in case of `static`)
or in another file (`extern`). You never need `static` declaration to compile with `c4wa` , but you might
need it for compatibility ith standard C compiler. `extern` declaration could come handy if you have more
than one source file to compile.

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
`double atan2(double, double)`, and then use this code in JavaScript to import:

```javascript
WebAssembly.instantiate(wasm_bytes, {c4wa: {atan2: Math.atan2}});
```

Any C attribute not listed above is not allowed 
(so you can't have `static` function definition or `extern` declaration).

## Memory

A simple C program might not require a linear memory at all; you can use 
[compiler option](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md) `module.memoryStatus=none`
to not add any memory declaration to generated WAT file. However, many features, such as 
taking address of a local variable, using `struct`s or arrays, calling imported function with 
arbitrary number of arguments (like `printf`), and obviosuly allocating memory directly, won't work without 
linear memory.

### Composition of linear memory

![Linear memory](https://github.com/kign/c4wa/blob/master/etc/doc/memory.png?raw=true "Linear memory" )

Generated WAT will have two special blocks of linear memory with configurable sizes.

  * **Stack**, size `module.stackSize` (default 1024), for stack variables;
  * **Data**, size `module.dataSize` (default 1024), for string literals.

The rest of memory, from byte number  `module.stackSize + module.dataSize` onwards, will only be used
by directly calling `alloc` pseudo-macro.

### `alloc` and `free`

You can think of `alloc` as macro wih this definition

```c
#ifdef C4WA
#define alloc(address, _ignore, type) (type *)(address + offset)
#else 
#define alloc(_ignore, count, type)  (type *)malloc((count) * (sizeof(type)))
#endif
```

In Web Assembly, this simply returns its first argument plus offset 
(which is where general use memory begins, `module.stackSize + module.dataSize`) wrapped up as a
pointer to the last argument (a type). When cross-compiling with regular C compiler, you can invoke `malloc`
to trigger a similar behaviour.

Example:

```c
int * arr = alloc(10, N + 1, int);
```

This can be used to allocate an array of `N+1` integers (note that you can also use syntax `int arr[N+1]` 
to allocate in the stack instead, subject to space limitations of course; see "stack arrays" below).

Second argument `N+1` is ignored by `c4wa`, so consider it a declaration of intent. 
It is only present for possible future implementation
of an actual memory manager and to make it easier to compile and test with native C compiler.

It's really up to you how you want to interpret `alloc` when cross-compiling with a native C compiler.
The easiest solution could be simply call `malloc` :

```c
#ifndef C4WA
#define alloc(_ignore, count, type)  (type *)malloc((count) * (sizeof(type)))
#endif
```

However, you may also choose to stay closer to Web Assembly linear memory model and do something like that:

```c
#ifndef C4WA
static void * __linear_memory;
#define alloc(addr, size, type) (type *)(__linear_memory + C4WA_STACK_SIZE  + C4WA_DATA_SIZE + addr)
#define free(addr)
#endif

//.........
extern int main () {
#ifndef C4WA
    __linear_memory = malloc(64000);
#endif
//.........
}
```

Function `free` basically has same semantic as in C standard library, it frees memory allocated earlier 
with `alloc`. Since for now `alloc` doesn't really allocate anything, `free` does nothing.
Just like 2<sup>nd</sup> argument to `alloc`, it is there for better compatibility with C compiler,
future enhancements and to better represent programmer's intent.

Note that function `free` is special in `c4wa` since it takes pointer of _any type_ as an argument.
Normally defined (or imported) functions would only work with a specific pointer type, including
built-in `memset` and `memcpy`.

Note also that while `alloc` is technically _not_ a function but a _pseudo-macro_ (it's last argument is type,
not a value), from this point on I'll be referring to it as one of the "built-in _functions_" for brevity.

### stack variables

Web Assembly supports unlimited number of local variables, so when you have a local variable in C,
we directly map it to a local variable in Web Assembly under same name (these names are only present in WAT file,
actual WASM code simply refers to them by consecutive numbers). 

In some situations though we have to store value in the _stack_, keeping its _address_ as a local variable.
In this case we refer to this variable as _stack variable_. This happens in these two cases:

First, this happens if you declare a variable of type array (not pointer) or `struct`. In this case,
your variable is allocated in the _stack_ (first `module.stackSize` bytes of linear memory). Actual WASM local variable
holds a _pointer_ to this memory (which is one case when pointer could have a numeric value 0, a very top of the stack).

### `&`

However, `cw4a` would also assign a regular primitive type variable to the stack _if you attempt to take an
address of this variable_. If everything works as it should, whether a local variable is allocated in the stack
or not should make no difference on the C code; however, constantly accessing and saving memory could 
have a performance hit and also generate larger and more complex WAT code. It is therefore recommended to
limit use of stack variables to get better results.

You can't take an address of an array (since it's already the address) or a global variable 
(since they can't be put on the stack)

One peculiarity of `c4wa` is that expression `&a[1]` is interpreted as `(&a)[1]` and not `&(a[1])` as it should.
This is related to left recursion in Antlr4, and I haven't been able to solve this yet without significant
changes to the grammar. For practical use, this is hardly a problem, you can always use parenthesis or
simply replace this expression with `a + 1`, which is what it is anyway.

### stack arrays

You can bypass manual memory allocation by using stack. When you declare a stack array, `type variable[size]`,
`size` doesn't have to be a compile-time constant, it could be any valid integer expression.

For example, if you need to allocate integer array of size `N` and fill it in with consecutive numbers `0 ... N-1`,
either of these two alternatives will work:

```c
int * arr = alloc(0, N, int);
for (int i = 0; i < N; i ++
    arr[i] = i; 
```

or

```c
int arr[N];
for (int i = 0; i < N; i ++
    arr[i] = i; 
```

in the 2<sup>nd</sup> version you don't need to worry about memory being available, not used by another object, and
marked as available again when no longer needed; however, you need to be mindful of available stack size (see above);
we are _not_ checking for stack overflow, so if you take too much memory you'd start overwriting your DATA section. 

Arrays are permitted in `struct`s, but must have fixed (= known at compile time) size. 

### Memory functions

These functions behave as normal C function in C code with given signatures 
(which are a bit different from C standard, effectively using `char *` as a generic pointer), 
but `c4wa` internally replaces them with Web Assembly memory operators:

| Name    | Arguments                               | Return Value | Description  | 
| ------- | --------------------------------------- | ------------ | ------------ |
| memset  | `char * addr`, `char value`, `int size` | _none_       | Same as in C |
| memcpy  | `char * dest`, `char * src`, `int size` | _none_       | Same as in C |
| memgrow | `int n_pages`                           | _none_       | Increase memory size by specified number of pages (1 page = 64K) |
| memsize | _none_                                  | `int`        | Get current memory size in pages |

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

## Built-in functions

In addition to memory functions `alloc` and `free` and memory functions `memset`, `memcpy`, `memgrow`, `memsize`, 
there are a few other built-in functions:

  * `min` and `max` work with any numerical arguments (of the same type), and will return result of the same type as arguments;
  * `floor`, `ceil`, `sqrt`, `fabs`. These functions work for `float` or `double` arguments, and will return same type as passed;
  * `abort` triggers "_RuntimeError: unreachable_" exception;
  * `__builtin_clz`, `__builtin_ctz`, `__builtin_clzl`, `__builtin_ctzl` 
    (see gcc [documentation](https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html)). Note that while
    in gcc behaviour is explicitly undefined if argument is 0 (in practice, implementations typically return 0), 
    in WASM these functions return full number of bits in the argument (so 32 for first two, 64 for the last). Note also
    that in GNU C compiler, builtin functions don't need to be declared, thus you don't need anything extra glue to
    cross-compile (just be mindful of argument 0).

## Strings and chars

Web Assembly has special DATA section and `data` instruction to store strings in memory. 
The capacity of DATA section is determined by `module.dataSize` compiler option; trying to
exceed this capacity will trigger a compiler error.

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

It must be acknowledged that `c4wa` isn't a good environment to write a code which deals with strings. 
This is in part because C itself isn't, and in part because working with strings means often 
allocation and freeing up memory, and you do need a decent memory manager for that.

## `printf`

`c4wa` doesn't have any built-in support for `printf` function; if you need, you can implement it in your runtime
and import into Web Assembly code. 

This however raises a problem how to deal with function with variable argument list. Normally,
an imported function must have a specific declared signature. We solve this problem by introducing special kind
of imported function, which could accept any number or type of arguments via memory pointers.

Specifically, if you declare a function _without signature_ (remember that a non-static declaration is considered
an import declaration):

```c
void printf();
```

generated WASM code would expect an imported function `printf` with **two** arguments: **memory address** to begin
reading arguments from and actual **number of arguments**; 
every argument takes exactly 8 bytes (64 bits) in linear memory.

If for example `printf` is actually called with 5 arguments, your implementation will receive two arguments,
let's call them `offset` and `count`; 
`count` will be number of arguments, 5 in this case, and values of these arguments will be stored in memory as follows:

1-st argument at address `offset`;<br>
2-nd argument at address `offset + 8`;<br>
3-rd argument at address `offset + 16`;<br>
4-th argument at address `offset + 24`;<br>
5-th argument at address `offset + 32`.

When passing arguments, all integer values are converted to `long`, and all float values to `double`.

Note that if passing a string as one of the arguments (as would be the case with an actual `printf` adaptation),
value stored in memory would _itself_ be a memory address of the string.

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

Booleans should be reasonably consistent with C: `0` is _false_, `1` is _true_, etc. We don't have any built-in
support for `true` or `false` constants, feel free to add your own via preprocessor or globals.

One thing to note, `c4wa` _does_ support proper semantics for boolean `&&` and `||` (so when evaluating
`A && B`, if `A` evaluates to _false_, `B` is not evaluated, similarly for `A || B`), 
but at a price of generating more complex WAT code 
(since there is no built-in support for such operations in Web Assembly). You may consider
using bitwise `&` and `|` instead in some situations, which directly translate to WASM instructions
resulting in simpler and faster code.

## Casts

As mentioned above, almost all casts must be explicit, and integer and float literals are `int` and `double`
respectively. `long` and `float` literals aren't supported, so to assign a constant to a float you need a cast:

```c
float x;
x = (float) 1; // or (float) 1.0;
double y;
y = 2. // or (double)2, but simply 2 won't work
```

## Globals

Web Assembly supports global variables, so you can freely use them in C code. However, Web Assembly requires that
all non-imported globals be initialized.

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

## C Preprocessor

While preprocessor is not part of `c4wa`, it can optionally run your code through external preprocessor
if available on the host, with `-P` command line option. Any command line option which begins with `-D` 
will be directly passed to the preprocessor (or ignored if `-P` isn't present). `-DC4WA` is always added;
use `-Ph` for the full list of predefined symbols.

`c4wa` is fully aware of so-called "line directives" inserted by a preprocessor and will use 
proper line numbers when reporting syntax errors.

## Cross-compiling with C

Cross-compiling the code with standard C compiler is listed above as one of the design goals, 
and it should indeed be simple and straightforward; `c4wa` does not use any special syntax or markers 
not known to a C compiler. Nevertheless, you may need a few adjustments:

  * `cw4a` is using some built-in functions specific to WASM and thus not present in standard library: 
    `alloc`, `memgrow`, `memsize` 
  * Other built-in functions while are part of C library, must be declared directly or via an include file;
  * `cw4a` is tolerant to functions calling each other in any order.

This is an example of the header you can include into your program:

```c
#ifndef C4WA
void * memset(); 
void * memcpy(); 
void * free(void *); 
double sqrt(double);
#define alloc(ignore, count, type)  (type *)malloc((count) * (sizeof(type)))
static int __memory_size = 1;
#define memgrow(size) __memory_size += (size)
#define memsize() __memory_size
#define min(a,b) ((a) < (b))?(a):(b)
#define max(a,b) ((a) < (b))?(b):(a)
#endif
```

## Comments

Both C-style `/* ... */` and C++ line comments `// ........` are supported.
