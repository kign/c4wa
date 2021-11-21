package net.inet_lab.c4wa.wat;

public class Rem extends Expression_2 {
    public Rem(NumType numType, boolean signed, Expression arg1, Expression arg2) {
        super((numType == NumType.F32 || numType == NumType.F64) ? InstructionName.REM :
                                (signed ? InstructionName.REM_S : InstructionName.REM_U),
                numType, arg1, arg2, null,null);
    }
}
