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
    : (STATIC|EXTERN)? CONST? variable_decl ('=' expression)? ';'                   # global_decl_variable
    | STATIC? variable_decl '(' (variable_type (',' variable_type)* )? ')' ';'    # global_decl_function
    | EXTERN? variable_decl '(' param_list? ')' composite_block                   # function_definition
    | STRUCT ID '{' (variable_decl ';')+ '}'  ';'                                 # struct_definition
    ;

variable_decl : primitive variable_with_modifiers;

variable_with_modifiers
    : ID                                 # variable_with_modifiers_name
    | '(' variable_with_modifiers ')'    # variable_with_modifiers_paren
    | variable_with_modifiers '[' ']'    # variable_with_modifiers_array
    | '*' variable_with_modifiers        # variable_with_modifiers_pointer
    ;

variable_type : primitive '*'* ;

primitive
    : UNSIGNED? (LONG | INT | SHORT | CHAR)     # integer_primitive
    | (DOUBLE | FLOAT)                          # float_primitive
    | VOID                                      # void_primitive
    | STRUCT ID                                 # struct_primitive
    ;

param_list : variable_decl (',' variable_decl)*;

composite_block: '{' element* '}';

block : composite_block | element;

element
    : ';'                                   # element_empty
    | statement ';'                         # element_statement
    | ASM                                   # element_asm
    | DO block WHILE '(' expression ')' ';' # element_do_while
    | FOR '(' pre=statement? ';' expression? ';' post=statement? ')' block  # element_for
    | IF '(' expression ')' (BREAK|CONTINUE) ';' # element_break_continue_if
    | (BREAK|CONTINUE) ';'                  # element_break_continue
    | IF '(' expression ')' block           # element_if
    | IF '(' expression ')' block ELSE block # element_if_else
    ;

statement
    : mult_variable_decl
    | variable_init
    | simple_assignment
    | complex_assignment
    | simple_increment
    | complex_increment
    | function_call
    | return_expression
    ;

mult_variable_decl : primitive variable_with_modifiers (',' variable_with_modifiers)* ;

variable_init : variable_decl '=' expression;

return_expression : RETURN expression;

simple_assignment : (ID '=')+ expression;

simple_increment: ID (op=(ASOR|ASAND|ASPLUS|ASMINUS|ASMULT|ASDIV|ASMOD) expression|PLUSPLUS|MINUSMINUS) ;

complex_increment: lhs op=(ASOR|ASAND|ASPLUS|ASMINUS|ASMULT|ASDIV|ASMOD) expression ;

complex_assignment: lhs '=' expression;

lhs
    : expression '->' ID                      # lhs_struct_member
    | ptr=expression '[' idx=expression ']'   # lhs_index
    | '*' expression                          # lhs_dereference
    ;

function_call : ID '(' arg_list? ')' ;

arg_list: expression (',' expression)*;

// cmp. Operators Precedence in C: https://www.tutorialspoint.com/Operators-Precedence-in-Cplusplus
expression
    : op=(NOT|MINUS|MULT) expression                 # expression_unary_op
    | expression '->' ID                      # expression_struct_member
    | SIZEOF '(' variable_type ')'                 # expression_sizeof_type
    | SIZEOF expression                 # expression_sizeof_exp
    | ptr=expression '[' idx=expression ']'        # expression_index
    | '(' expression ')'                  # expression_parentheses
    | '(' variable_type ')' expression    # expression_cast
    | expression op=(MULT | DIV | MOD) expression    # expression_binary_mult
    | expression op=(PLUS | MINUS) expression                        # expression_binary_add
    | expression op=(LTEQ | GTEQ | LT | GT | EQ | NEQ) expression    # expression_binary_cmp
    | expression op=AND expression           # expression_binary_and
    | expression op=OR expression            # expression_binary_or
    | expression '?' expression ':' expression # expression_if_else
    | CONSTANT                            # expression_const
    | ID                                  # expression_variable
    | STRING                              # expression_string
    | CHARACTER                              # expression_character
    | ALLOC '(' memptr=expression ',' count=expression ',' variable_type ')' # expression_alloc
    | function_call                       # expression_function_call
    ;


OR : '||';
AND : '&&';
PLUS : '+';
MINUS : '-';
MULT : '*';
DIV : '/';
MOD : '%';
ASOR : '||=';
ASAND : '&&=';
ASPLUS : '+=';
ASMINUS : '-=';
ASMULT : '*=';
ASDIV : '/=';
ASMOD : '%=';
PLUSPLUS : '++';
MINUSMINUS: '--';

EQ : '==';
NEQ : '!=';
GT : '>';
LT : '<';
GTEQ : '>=';
LTEQ : '<=';
POW : '^';
NOT : '!';
MEMB : '->';

Q: '?';
COL: ':';
SCOL : ';';
ASSIGN : '=';
OPAR : '(';
CPAR : ')';
OBRACE : '{';
CBRACE : '}';
OBRAKET : '[';
CBRACKET : ']';

TRUE : 'true';
FALSE : 'false';
NIL : 'nil';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
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
SIZEOF :  'sizeof';

ALLOC  :  'alloc';

ID     :  [a-zA-Z_][a-zA-Z0-9_]*;
ASM    :  'asm' [ \t\n\r]* '{' .*? '}';

CONSTANT : Sign? Constant;

STRING
    :   '"' SCharSequence? '"'
    ;

CHARACTER : '\'' SChar '\'' ;

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
