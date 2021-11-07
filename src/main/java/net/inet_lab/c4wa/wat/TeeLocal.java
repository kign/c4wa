package net.inet_lab.c4wa.wat;

public class TeeLocal extends Instruction_SetLocal {
    public TeeLocal(String ref, Instruction arg) {
        super(InstructionName.TEE_LOCAL, ref, arg);
    }
}
