package net.inet_lab.c4wa.wat;

public class Store extends Instruction_cls2 {
    final public NumType numType;

    public Store(NumType numType, Instruction offset, Instruction value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.STORE), offset, value);
        this.numType = numType;
    }
}
