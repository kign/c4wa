grammar c4wa;

@header {
package net.inet_lab.c4wa.autogen.parser;
}
// https://github.com/antlr/grammars-v4/blob/master/c/C.g4
// Also good reference: https://github.com/bkiers/Mu/blob/master/src/main/antlr4/mu/Mu.g4

module
    : global_decl+ EOF
    ;

global_decl
    : (STATIC|EXTERN)? CONST? variable_decl ('=' CONSTANT)? ';'                            # global_decl_variable
    | STATIC? variable_decl '(' (variable_type (',' variable_type)* )? ')' ';'    # global_decl_function
    | EXTERN? variable_decl '(' param_list? ')' composite_block # function_definition
    ;

variable_decl : variable_type ID;

variable_type
    : primitive          # type_primitive
    | primitive '*'      # type_pointer_to_primitive
    | STRUCT '*'         # type_pointer_to_struct
    ;

primitive
    : integer_primitive
    | float_primitive
    | void_primitive
    ;

integer_primitive : UNSIGNED? (LONG | INT | SHORT | CHAR);

float_primitive : DOUBLE | FLOAT;

void_primitive: VOID;

param_list : variable_decl (',' variable_decl)*;

composite_block: '{' element* '}';

block : composite_block | element;

element
    : ';'                                   # element_empty
    | statement ';'                         # element_statement
    | ASM                                   # element_asm
    | DO block WHILE '(' expression ')' ';' # element_do_while
    | FOR '(' pre=statement? ';' expression? ';' post=statement? ')' block  # element_for
    | IF '(' statement ')' (BREAK|CONTINUE) ';' # element_break_continue_if
    | (BREAK|CONTINUE) ';'                  # element_break_continue
    | IF '(' expression ')' block           # element_if
    | IF '(' expression ')' block ELSE block # element_if_else
    ;

statement
    : mult_variable_decl
    | variable_init
    | simple_assignment
    | complex_assignment
    | function_call
    | return_expression
    ;

mult_variable_decl : variable_type ID (',' ID)* ;

variable_init : variable_decl '=' expression;

return_expression : RETURN expression;

simple_assignment : (ID '=')+ expression;

complex_assignment: lhs '=' expression;

lhs
    : expression '->' ID
    | expression '[' expression ']'
    ;

function_call : ID '(' arg_list? ')' ;

arg_list: expression (',' expression)*;

expression
    : op=(NOT|MINUS) expression                 # expression_unary_op
    | '(' expression ')'                  # expression_parentheses
    | '(' variable_type ')' expression    # expression_cast
    | expression op=(MULT | DIV | MOD) expression    # expression_binary_mult
    | expression op=(PLUS | MINUS) expression                        # expression_binary_add
    | expression op=(LTEQ | GTEQ | LT | GT | EQ | NEQ) expression    # expression_binary_cmp
    | expression op=AND expression           # expression_binary_and
    | expression op=OR expression            # expression_binary_or
    | CONSTANT                            # expression_const
    | ID                                  # expression_variable
    | STRING                              # expression_string
    | function_call                       # expression_function_call
    ;


OR : '||';
AND : '&&';
EQ : '==';
NEQ : '!=';
GT : '>';
LT : '<';
GTEQ : '>=';
LTEQ : '<=';
PLUS : '+';
MINUS : '-';
MULT : '*';
DIV : '/';
MOD : '%';
POW : '^';
NOT : '!';

SCOL : ';';
ASSIGN : '=';
OPAR : '(';
CPAR : ')';
OBRACE : '{';
CBRACE : '}';

TRUE : 'true';
FALSE : 'false';
NIL : 'nil';
IF : 'if';
ELSE : 'else';
WHILE : 'while';

//MULT : '*';
//DIV : '/';
//MOD : '%';
// BINARY_OP2 : Star | '/' | '%'; // somehow directly inserting * isn't working
// Star : '*';
// BINARY_OP1 : '+'|'-';
//BINARY_OP0 : '<'|'>'|'<='|'>='|'=='|'!=';
//PLUS   :  '+';
// MINUS  :  '-';
UNSIGNED : 'unsigned';
LONG   :  'long';
INT    :  'int';
SHORT  :  'short';
CHAR   :  'char';
STRUCT :  'struct';
RETURN :  'return';
EXTERN :  'extern';
STATIC :  'static';
CONST  :  'const';
DO     :  'do';
//WHILE  :  'while';
FOR    :  'for';
BREAK  :  'break';
CONTINUE : 'continue';
//IF     :  'if';
//ELSE   :  'else';
DOUBLE :  'double';
FLOAT  :  'float';
VOID   :  'void';

ID     :  [a-zA-Z_][a-zA-Z0-9_]*;
ASM    :  'asm' [ \t\n\r]* '{' .*? '}';

CONSTANT : Sign? Constant;

STRING
    :   '"' SCharSequence? '"'
    ;

fragment
Constant
    :   '0'
    |   DecimalConstant
    |   DecimalFloatingConstant
    ;

fragment
DecimalConstant
    :   NonzeroDigit Digit*
    ;

fragment
DecimalFloatingConstant
    :   FractionalConstant ExponentPart?
    |   DigitSequence ExponentPart
    ;

fragment
NonzeroDigit
    :   [1-9]
    ;

fragment
Digit
    :   [0-9]
    ;

DigitSequence
    :   Digit+
    ;

fragment
FractionalConstant
    :   DigitSequence? '.' DigitSequence
    |   DigitSequence '.'
    ;

fragment
ExponentPart
    :   [eE] Sign? DigitSequence
    ;

Sign
    :   [+-]
    ;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\\\r\n]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   SimpleEscapeSequence
    |   OctalEscapeSequence
    |   HexadecimalEscapeSequence
    ;
fragment
SimpleEscapeSequence
    :   '\\' ['"?abfnrtv\\]
    ;
fragment
OctalEscapeSequence
    :   '\\' OctalDigit OctalDigit? OctalDigit?
    ;
fragment
HexadecimalEscapeSequence
    :   '\\x' HexadecimalDigit+
    ;

fragment
OctalDigit
    :   [0-7]
    ;

fragment
HexadecimalDigit
    :   [0-9a-fA-F]
    ;

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
