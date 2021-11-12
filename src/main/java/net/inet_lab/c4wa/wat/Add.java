package net.inet_lab.c4wa.wat;

public class Add extends Instruction_Bin {
    final public NumType numType;
    public Add(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.ADD), arg1, arg2, Long::sum, Double::sum);
        this.numType = numType;
    }
}
