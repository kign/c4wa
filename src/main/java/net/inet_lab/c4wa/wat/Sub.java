package net.inet_lab.c4wa.wat;

public class Sub extends Instruction_Add {
    final public NumType numType;

    public Sub(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.SUB), arg1, arg2);
        this.numType = numType;
    }
}
