package net.inet_lab.c4wa.wat;

public class Br extends Instruction_GetLocal {
    public Br(String ref) {
        super(InstructionName.BR, ref);
    }
}
