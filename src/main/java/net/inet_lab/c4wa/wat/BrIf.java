package net.inet_lab.c4wa.wat;

public class BrIf extends Instruction_SetLocal {
    public BrIf(String ref, Instruction arg) {
        super(InstructionName.BR_IF, ref, arg);
    }
}
