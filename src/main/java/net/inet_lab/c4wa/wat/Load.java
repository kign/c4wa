package net.inet_lab.c4wa.wat;

public class Load extends Expression_1 {
    public Load(NumType numType, Expression arg) {
        super(InstructionName.LOAD, numType, arg);
    }

    public Load(NumType numType, int wrap, boolean signed, Expression arg) {
        super((wrap == 8) ?   (signed? InstructionName.LOAD8_S : InstructionName.LOAD8_U) :
            ((wrap == 16) ? (signed ? InstructionName.LOAD16_S : InstructionName.LOAD16_U) :
                            (signed ? InstructionName.LOAD32_S : InstructionName.LOAD32_U)),
        numType, arg);
    }
}
