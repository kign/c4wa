package net.inet_lab.c4wa.wat;

public class Div extends Expression_2 {
    public Div(NumType numType, boolean signed, Expression arg1, Expression arg2) {
        super((numType == NumType.F32 || numType == NumType.F64)?InstructionName.DIV:
                        (signed?InstructionName.DIV_S: InstructionName.DIV_U),
                numType, arg1, arg2, (a,b) -> a / b, (a,b) -> a / b);
    }
}
