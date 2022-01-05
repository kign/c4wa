package net.inet_lab.c4wa.wat;

public class BrIf extends Instruction_1ref {
    public BrIf(String ref, Expression condition) {
        super(InstructionName.BR_IF, ref, condition);
    }
}
