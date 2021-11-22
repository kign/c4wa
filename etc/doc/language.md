# Language Spec

`c4wa` compiler operates on a subset of C language. In this section, we attempt to describe this subset.
Obviously, details might change as work on the compiler continues.

## Design goals

  * **Direct translation.**<br> We try to only support features of C language which could be directly and unambiguously
    translated to WAT text format with S-expressions. This way, generated WAT code should be readable and reasonably close to what 
    a human programmer would write.
  * **Functionality first, syntax sugar later.**<br> Implement the widest possible scope of languages features first, worry 
    about convenience only as necessary.
  * **Compatibility with C standard.**<br> It shouldn't take too much effort to write code which could be compiled 
    and tested with an ordinary C compiler.

## What this compiler is NOT

Just to set expectations correctly, a few things **can't** be guaranteed due to limited scope of this project, 
unavoidable inconsistencies with the standard C compiler, and differences between native executable and 
Web Assembly environment.

  * Your code might successfully pass through `c4wa` but still fail `wat2wasm` compilation. 
    The plan in to eventually try to verify generated code as much as possible to avoid incorrect WAT output, 
    but it's still very much work in progress (for example, 
    if you fail to return a value from a non-void function, you'll get a error from `wat2wasm`, not from `c4wa`). 
    Fortunately, since generated WAT code could be easily matched to the original code, 
    such errors are easy to fix in the original C code (unless it is a compiler bug)
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
    or libraries, doesn't use any compiler- or OS-specific features, and doesn't use too much certain more obscure 
    language features, making it `c4wa`-compatible shouldn't take too much effort. 

## Overview

Web Assembly doesn't have any memory management features, instead giving programmers access to one single memory block 
("linear memory"). Correspondingly, `c4wa` has limited any dynamic memory allocation capabilities; for anything beyond
that, developer must assign free space to all dynamic objects manually (more on tha below).

`cw4a` supports Web Assembly primitive types (`i32`, `i64`, `f32` and `f64` which translate to C as 
`int`, `long`, `float` and `double` correspondingly), also `char` and `short` 
(which are internally `i32`, except for `struct` members, also some operations work for them differently), 
pointers, structures and arrays. Not all possible combinations are supported though, like you can't have
pointer to an array, etc.

Any integer types could be `unsigned`. `sizeof` is supported (but will return results different from native C
compiler due to no need to use alignment in Web Assembly memory).

`typedef` isn't supported. You must use syntax `struct NAME` when declaring variables of type `struct`. 
There are no `union`s.

`c4wa` supports most usual operators, but assignment isn't treated as an operator. It does support chain assignment
`a = b = c ...` with some limitations though.

Usual pointer arithmetic is supported; `&` operator can be used with some limitations.

There are no `void *` pointers and no `NULL`. If you are using a "generic" pointer, you must explicitly cast it.
Built-in functions like `memcpy` use `char *`.  0 is a valid pointer value (not for dynamically allocated objects
but for addresses of local variables); you can if you wish compare your pointer with `(type *) 0`, but if you fail to
zero-test, your code will still execute and might not do what you intended.

Almost all arithmetic operations, function calls and assignment require explicit type cast if types are inconsistent.

Local variables can be introduced anywhere in the program, but all share scope of the function. There aren't 
block-level locals.

The ony "native" loop type in Web Assembly is `do ... while()`; you are encouraged to use it whenever practical 
since this creates cleaner and simpler WAT/WASM code. Since it is so common C, we do 
nevertheless support a regular `for` loop, but not `while() {   }` loop. Use `for(; ... ;)` syntax if you must.

There is no comma `,` operator in `c4wa` (so you can't for example have multiple initializations in `for` loop).

You can define multiple variables in one definition like `int *a, b, c[2]` and you can 
initialize variables when you define them, e.g.
`int * x = alloc_smth()`, but not both at the same time. There is no initialization syntax for arrays or
structures (except literal strings, which are zero-terminated `char` arrays); neither arrays nor `struct`s 
could be assigned to.

Web Assembly is kind of strict about requiring explicit `return` statement at the end of non-void functions,
even if your code is structured in a way that it could never reach the end, and value is ways returned; 
this requirement is thus passed to code written for `c4wa` (I could of course add missing `return` to generated WAT code,
but that would risk masking a real problem, so I prefer not to do it).

Function declarations (unlike definition) can't have parameter names, only types.

## Import and export

Syntax of C doesn't exactly match Web Assembly notion of "imported" and "exported" symbols (global variables, functions
and memory); here is how we map existing C attributes Web Assembly:

**Function definition** could be `extern`; this makes function exported. It is always exported under its own name. 
A function which is not `extern` will not be exported. Obviously, you should always have at least one `extern` function,
but you can have as many as you want.

**Function declaration** could be `static`; this will case declared function _not_ to be imported. 

Normally, when you declare function like `double atan2(double, double)`, it is interpreted as imported, and
if not provided by the run-time, this will trigger a error. You don't have to declare a function defined 
in your code, regardless where it is defined or used; however, normal C compiler might require such 
preliminary declaration. In this case, you can declare `static` function, so it won't be looked up in import.

**Global variables** could be `static`, `extern`, or neither. `extern` variable will be exported, and neither `extern`
nor `static` will be imported (just like a function declaration). `static` global variables are neither 
imported not exported.

(Unrelated to import or export, global variable could also be `const`).

**Memory** behaviour is determined by compiler options 
(see [here](https://github.com/kign/c4wa/blob/master/etc/doc/properties.md)). It could be imported, 
exported (current default), purely internal or not be present at all.

All exported objects are exported and imported under there names in C (except memory, which doesn't have a C
identifier and so name is determine by compiler options). When importing, module name is also set by compiler 
options (default is `c4wa`).

## Memory

### Composition of linear memory

### `alloc`

### stack variables

### `&`

## Strings and chars

## `printf`

## Memory operations

## Boolean operators and values

## Casts
