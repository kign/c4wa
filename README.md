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
some other syntax sugar to make coding easier. 
See excellent [introduction](https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format) 
to WAT format at MDN. 

`c4wa` purports to be a middle ground between these two extremes. It allows you to write a code in a 
relatively higher-level language (a subset of `C`) while retaining a close relation to an underlying
Web Assembly. Instead of a binary WASM file, it generates a well-formatted WAT output 
which is trying to be similar to what a human programmer would have written when solving the problem directly in WAT.

## Installation

## Usage