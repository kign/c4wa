package net.inet_lab.c4wa.wat;

public class Neg extends Instruction_Return {
    public Neg(NumType numType, Instruction arg) {
        super(new InstructionWithNumPrefix(numType, InstructionName.NEG), arg);
    }

    @Override
    public Instruction comptime_eval() {
        if (arg instanceof Const) {
            Const a = (Const) arg;

            return new Const(type.getPrefix(), -((Const)arg).doubleValue);
        }
        return this;
    }
}
