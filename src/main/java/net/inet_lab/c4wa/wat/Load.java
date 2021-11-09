package net.inet_lab.c4wa.wat;

public class Load extends Instruction_Return {
    public Load(NumType numType, Instruction arg) {
        super(new InstructionWithNumPrefix(numType, InstructionName.LOAD), arg);
    }

    public Load(NumType numType, int wrap, boolean signed, Instruction arg) {
        super(new InstructionWithNumPrefix(numType,
            (wrap == 8) ?   (signed? InstructionName.LOAD8_S : InstructionName.LOAD8_U) :
            ((wrap == 16) ? (signed ? InstructionName.LOAD16_S : InstructionName.LOAD16_U) :
                            (signed ? InstructionName.LOAD32_S : InstructionName.LOAD32_U))),
        arg);
    }
}
