package net.inet_lab.c4wa.wat;

public class Sqrt extends Expression_1 {
    public Sqrt(NumType numType, Expression arg) {
        super(InstructionName.SQRT, numType, arg);
    }
}
