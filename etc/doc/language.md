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
unavoidable inconsistencies with the standard C compiler, and differences between native executable and Web Assembly environment.

  * Your code might successfully pass through `c4wa` but still fail `wat2wasm` compilation. 
    The plan in to eventually try to verify generated code as much as possible to avoid incorrect WAT output, 
    but it's still very much work in progress (for example, as of version 0.1, 
    if you fail to return a value from a non-void function, you'll get a error from `wat2wasm`, not from `c4wa`). 
    In the meantime, since generated WAT code could be easily matched to the original code, 
    such errors are easy to fix in the original C code.
  * Your code might successfully compile with both regular compiler and `c4wa`, generate correctly working
    native executable, but still work incorrectly in Web Assembly. This could be due to a limited number of known
    inconsistencies you should be aware of when writing code for `c4wa`.
  * There could be occasional instances when due to a bug native executable would fail, yet Web Assembly version
    would still work as expected.
  * `cw4a` might support certain features your C compiler does not, thus your code could compile and correctly execute
    in Web Assembly, yet still require some adaptation to pass through C compiler.

## Overview

Web Assembly doesn't have any memory management features, instead giving programmers access to one single memory block ("linear memory").
Correspondingly, `c4wa` doesn't have any dynamic memory allocation capabilities, but instead developer
must explicitly assign memory addresses to all dynamic objects (other than local or global variables of primitive types).
More on that below.

`cw4a` supports Web Assembly primitive types (`i32`, `i64`, `f32` and `f64` which translate to C as `int`, `long`, `float` and `double`
correspondingly), also `char` and `short` (which are internally `i32`, except for `struct` members, 
also some operations work for them differently), 
pointers and structures. Array syntax is just an alias for a pointer. Integer types could be `unsigned`. 
`sizeof` is supported.

`typedef` isn't supported. You must use syntax `struct NAME` when declaring variables of type `struct`. 
There are no `union`s.

`c4wa` supports most usual operators, but assignment isn't treated as an operator. It does support chain assignment
`a = b = c ...` with some limitations though.

Usual pointer arithmetic is supported, but there isn't `&` operator (since you can't take address of a local variable).

There are no `void *` pointers and no `NULL`. If you are using a "generic" pointer, you must explicitly cast it.
0 is a valid pointer value (well not exactly but more on that below); 
you can if you wish compare your pointer with `(type *) 0`, but if you fail to
zero-test, your code will still execute and might not do what you intended.

Almost all arithmetic operations, function calls and assignment require explicit type cast if types are inconsistent.

Local variables can be introduced anywhere in the program, but all share scope of the function. There aren't 
block-level locals.

The ony "native" loop type in Web Assembly is `do ... while()`; you are encouraged to use it whenever practical 
since this creates cleaner and simpler WAT/WASM code. Since it is so common C, we do 
nevertheless support a regular `for` loop, but not `while() {   }` loop. Use `for(; ... ;)` syntax if you must.

There is no comma `,` operator in `c4wa`.




