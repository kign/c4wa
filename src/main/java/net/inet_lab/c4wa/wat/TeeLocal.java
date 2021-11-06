package net.inet_lab.c4wa.wat;

public class TeeLocal extends Instruction_GetLocal {
    public TeeLocal(String ref) {
        super(InstructionName.TEE_LOCAL, ref);
    }
}
