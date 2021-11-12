package net.inet_lab.c4wa.wat;

public class Div extends Instruction_Bin {
    public Div(NumType numType, boolean signed, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType,
                (numType == NumType.F32 || numType == NumType.F64)?InstructionName.DIV:
                        (signed?InstructionName.DIV_S: InstructionName.DIV_U)),
                arg1, arg2, (a,b) -> a / b, (a,b) -> a / b);
    }
}
