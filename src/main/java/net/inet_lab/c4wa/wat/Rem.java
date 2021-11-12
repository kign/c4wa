package net.inet_lab.c4wa.wat;

public class Rem extends Instruction_Bin {
    public Rem(NumType numType, boolean signed, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType,
                        (numType == NumType.F32 || numType == NumType.F64) ? InstructionName.REM :
                                (signed ? InstructionName.REM_S : InstructionName.REM_U)),
                arg1, arg2,null,null);
    }
}
