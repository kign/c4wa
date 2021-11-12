package net.inet_lab.c4wa.wat;

public class Sub extends Instruction_Bin {
    final public NumType numType;

    public Sub(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.SUB), arg1, arg2, (a,b)->a-b, (a,b)->a-b);
        this.numType = numType;
    }
}
