grammar c4wa;

// make sure "package" matches "generateGrammarSource" target in `build.gradle`
@header {
package net.inet_lab.c4wa.autogen.cparser;
}
// https://github.com/antlr/grammars-v4/blob/master/c/C.g4
// Also good reference: https://github.com/bkiers/Mu/blob/master/src/main/antlr4/mu/Mu.g4

module
    : global_decl+ EOF
    ;

global_decl
    : (STATIC|EXTERN)? CONST? variable_decl ('=' expression)? ';'                                    # global_decl_variable
    | (STATIC|EXTERN)? variable_decl '(' (variable_type (',' variable_type)* (',' '...')? )? ')' ';' # global_decl_function
    | EXTERN? variable_decl '(' param_list? ')' composite_block                                      # function_definition
    | STRUCT ID '{' (struct_mult_members_decl ';')+ '}'  ';'                                         # struct_definition
    ;

param_list : variable_decl (',' variable_decl)* (',' '...')?;

variable_decl : primitive variable_with_modifiers;

variable_with_modifiers
    : ID                                 # variable_with_modifiers_name
    | '(' variable_with_modifiers ')'    # variable_with_modifiers_paren
    | variable_with_modifiers '[' CONSTANT? ']'  # variable_with_modifiers_array
    | '*' variable_with_modifiers        # variable_with_modifiers_pointer
    ;

local_variable:  '*'* ID ('[' expression ']')?;

struct_member_decl:  '*'* ID ('[' expression ']')?;

struct_mult_members_decl : primitive struct_member_decl (',' struct_member_decl)* ;

variable_type : primitive '*'* ;

primitive
    : UNSIGNED? (LONG | INT | SHORT | CHAR)     # integer_primitive
    | (DOUBLE | FLOAT)                          # float_primitive
    | VOID                                      # void_primitive
    | STRUCT ID                                 # struct_primitive
    ;

composite_block: '{' element* '}';

block : composite_block | element;

element
    : ';'                                   # element_empty
    | statement ';'                         # element_statement
    | DO block WHILE '(' expression ')' ';' # element_do_while
    | FOR '(' pre=statement? ';' expression? ';' post=statement? ')' block  # element_for
    | IF '(' expression ')' (BREAK|CONTINUE) ';' # element_break_continue_if
    | (BREAK|CONTINUE) ';'                  # element_break_continue
    | IF '(' expression ')' block           # element_if
    | IF '(' expression ')' block ELSE block # element_if_else
    | composite_block                       # element_block
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
    | statement ',' statement
    ;

mult_variable_decl : primitive local_variable (',' local_variable)* ;

variable_init : CONST? variable_decl '=' expression;

return_expression : RETURN expression?;

simple_assignment : (ID '=')+ expression;

simple_increment: ID  (op=(ASOR|ASAND|ASPLUS|ASMINUS|ASMULT|ASDIV|ASMOD|ASBWAND|ASBWOR|ASBWXOR) expression|PLUSPLUS|MINUSMINUS) ;

complex_increment: lhs (op=(ASOR|ASAND|ASPLUS|ASMINUS|ASMULT|ASDIV|ASMOD|ASBWAND|ASBWOR|ASBWXOR) expression|PLUSPLUS|MINUSMINUS) ;

complex_assignment: lhs '=' expression;

lhs
    : expression '->' ID                      # lhs_struct_member
    | expression '.' ID                      # lhs_struct_member_dot
    | '(' lhs ')'                      # lhs_parentheses
    | ptr=expression '[' idx=expression ']'   # lhs_index
    | '*' expression                          # lhs_dereference
    ;

function_call : ID '(' arg_list? ')' ;

arg_list: expression (',' expression)*;

// cmp. Operators Precedence in C: https://www.tutorialspoint.com/Operators-Precedence-in-Cplusplus
expression
    : ptr=expression '[' idx=expression ']'                 # expression_index
    | expression '->' ID                                    # expression_struct_member
    | expression '.' ID                                     # expression_struct_member_dot
    | op=(NOT|MINUS|MULT|TILDA) expression                  # expression_unary_op
    | BWAND ID                                              # expression_addr_var
    | BWAND lhs                                             # expression_addr_lhs
    | SIZEOF '(' variable_type ')'                          # expression_sizeof_type
    | SIZEOF expression                                     # expression_sizeof_exp
    | '(' expression ')'                                    # expression_parentheses
    | '(' variable_type ')' expression                      # expression_cast
    | expression op=(MULT | DIV | MOD) expression           # expression_binary_mult
    | expression op=(PLUS | MINUS) expression               # expression_binary_add
    | expression op=(LSHIFT | RSHIFT) expression            # expression_binary_shift
    | expression op=(LTEQ | GTEQ | LT | GT) expression      # expression_binary_cmp
    | expression op=(EQ | NEQ) expression                   # expression_binary_equal
    | expression op=BWAND expression                        # expression_binary_bwand
    | expression op=BWXOR expression                        # expression_binary_bwxor
    | expression op=BWOR expression                         # expression_binary_bwor
    | <assoc=right>left=expression AND right=expression     # expression_binary_and
    | <assoc=right>left=expression OR right=expression      # expression_binary_or
    | <assoc=right>expression '?' expression ':' expression # expression_if_else
    | CONSTANT                                              # expression_const
    | ID                                                    # expression_variable
    | STRING+                                               # expression_string
    | CHARACTER                                             # expression_character
    | function_call                                         # expression_function_call
    | left_side_of_comma ',' expression  # expression_comma
    ;

left_side_of_comma
    : simple_assignment
    | simple_increment
    | function_call
    ;


OR : '||';
AND : '&&';
BWOR : '|';
BWAND : '&';
TILDA: '~';
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
ASBWOR : '|=';
ASBWAND : '&=';
ASBWXOR : '^=';
PLUSPLUS : '++';
MINUSMINUS: '--';
LSHIFT: '<<';
RSHIFT: '>>';
EQ : '==';
NEQ : '!=';
GT : '>';
LT : '<';
GTEQ : '>=';
LTEQ : '<=';
BWXOR : '^';
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
DOT: '.';
ELLIPSIS : '...';

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
FOR    :  'for';
BREAK  :  'break';
CONTINUE : 'continue';
DOUBLE :  'double';
FLOAT  :  'float';
VOID   :  'void';
SIZEOF :  'sizeof';

ID     :  [a-zA-Z_][a-zA-Z0-9_]*;

STRING
    :   '"' SCharSequence? '"'
    ;

CHARACTER : '\'' SChar '\'' ;

CONSTANT
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

// These two line directives are copy-pasted from C.g4 (see above), but I don't get the logic
// behind including "#line". Isn't it a preprocessor instruction? What is it doing here?
LineAfterPreprocessing
    :   '#line' Whitespace* ~[\r\n]*
        -> skip
    ;

// These are inserted by C preprocessor. We ignore it here, but use
// these markers to map line numbers reported by parser to "real" line numbers
LineDirective
    :   '#' Whitespace? DecimalConstant Whitespace? STRING ~[\r\n]*
        -> skip
    ;
