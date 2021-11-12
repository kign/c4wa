package net.inet_lab.c4wa.wat;

public class And extends Instruction_Bin {
    final public NumType numType;

    public And(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.AND), arg1, arg2, (a,b) -> a&b, null);
        this.numType = numType;
    }

    @Override
    public Instruction Not(NumType numType) {
        return new Or(numType, arg1.Not(numType), arg2.Not(numType));
    }
}
