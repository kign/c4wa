package net.inet_lab.c4wa.wat;

public class Or extends Instruction_Add {
    final public NumType numType;

    public Or(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.OR), arg1, arg2);
        this.numType = numType;
    }

    @Override
    public Instruction Not(NumType numType) {
        return new And(numType, arg1.Not(numType), arg2.Not(numType));
    }
}
