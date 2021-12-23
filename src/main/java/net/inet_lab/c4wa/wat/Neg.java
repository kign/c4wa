package net.inet_lab.c4wa.wat;

public class Neg extends Expression_1 {
    public Neg(NumType numType, Expression arg) {
        super(InstructionName.NEG, numType, arg);
    }

    @Override
    public Expression comptime_eval() {
        if (arg instanceof Const)
            return new Const(numType, -((Const)arg).doubleValue);
        return this;
    }
}
