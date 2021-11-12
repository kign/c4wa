package net.inet_lab.c4wa.wat;

public class Xor extends Instruction_Bin {
    final public NumType numType;

    public Xor(NumType numType, Instruction arg1, Instruction arg2) {
        super(new InstructionWithNumPrefix(numType, InstructionName.XOR), arg1, arg2, (a,b)->a^b,null);
        this.numType = numType;
    }
}
