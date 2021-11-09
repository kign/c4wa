package net.inet_lab.c4wa.wat;

public class Store extends Instruction_Add {
    final public NumType numType;

    public Store(NumType numType, Instruction offset, Instruction value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.STORE), offset, value);
        this.numType = numType;
    }

    public Store(NumType numType, int wrap, Instruction offset, Instruction value) {
        super(new InstructionWithNumPrefix(numType,
                (wrap == 8)? InstructionName.STORE8 : ((wrap == 16) ? InstructionName.STORE16 : InstructionName.STORE32)),
                offset, value);
        this.numType = numType;
    }
}
