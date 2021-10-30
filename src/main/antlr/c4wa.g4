grammar c4wa;

@header {
   package net.inet_lab.c4wa.autogen.parser;
}
// https://github.com/antlr/grammars-v4/blob/master/c/C.g4

module
    : global_decl* func+ EOF
    ;

global_decl
    : variable_decl ';'
    | EXTERN? variable_decl '(' param_list? ')' ';'
    ;

variable_decl
    : primitive ID
    | primitive '*' ID
    | STRUCT '*' ID
    ;

primitive : UNSIGNED? (LONG | INT | SHORT | CHAR);

func : EXTERN? variable_decl '(' param_list? ')' '{' element* '}';

param_list : variable_decl (',' variable_decl);

element
    : statement ';'
    | ASM
    ;

statement
    : variable_decl
    | simple_assignment
    | complex_assignment
    | function_call
    | RETURN expression
    ;

simple_assignment : (ID '=')+ expression;

complex_assignment: lhs '=' expression;

lhs
    : expression '->' ID
    | expression '[' expression ']'
    ;

function_call : ID '(' arg_list? ')' ;

arg_list: expression (',' expression)*;

expression
    : CONST
    | ID
    | expression BINARY_OP expression
    | function_call
    ;

CONST : [0-9]+;
BINARY_OP : '+'|'-'|'*'|'/'|'%';
PLUS   :  '+';
MINUS  :  '-';
UNSIGNED : 'unsigned';
LONG   :  'long';
INT    :  'int';
SHORT  :  'short';
CHAR   :  'char';
STRUCT :  'struct';
RETURN :  'return';
EXTERN :  'extern';
ID     :  [a-zA-Z_][a-zA-Z0-9_]*;
ASM    :  'asm' [ \t\n\r]* '{' .*? '}';

Whitespace
    :   [ \t]+
        -> skip
    ;

Newline
    :   (   '\r' '\n'?
        |   '\n'
        )
        -> skip
    ;

BlockComment
    :   '/*' .*? '*/'
        -> skip
    ;

LineComment
    :   '//' ~[\r\n]*
        -> skip
    ;
