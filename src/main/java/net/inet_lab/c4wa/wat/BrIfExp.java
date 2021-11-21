package net.inet_lab.c4wa.wat;

public class BrIfExp extends Expression_2ref {
    public BrIfExp(String ref, NumType numType, Expression condition, Expression returnValue) {
        super(InstructionName.BR_IF, numType, ref, returnValue, condition);
    }
}
