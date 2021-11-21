package net.inet_lab.c4wa.wat;

public class Eqz extends Expression_1 {
    public Eqz(NumType numType, Expression arg) {
        super(InstructionName.EQZ, numType, arg);
    }

    @Override
    public Expression Not(NumType numType) {
        return arg;
    }
}
