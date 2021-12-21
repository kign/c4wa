package net.inet_lab.c4wa.wat;

public class Shift extends Expression_2 {
    public Shift(NumType numType, boolean isLeft, boolean isSigned, Expression arg1, Expression arg2) {
        super(isLeft? InstructionName.SHL: (isSigned? InstructionName.SHR_S: InstructionName.SHR_U), numType,
                arg1, arg2, isLeft? ((isSigned && numType==NumType.I32)? (a, b) -> (long)((int) a << (int)b): (a, b) -> a << b): (a, b) -> a >> b, null);
    }
}
