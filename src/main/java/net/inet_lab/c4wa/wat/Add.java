package net.inet_lab.c4wa.wat;

public class Add extends Instruction_cls2 {
    final public NumType numType;
    public Add(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.ADD), arg1, arg2);
        this.numType = numType;
    }
}
