package net.inet_lab.c4wa.wat;

public class Loop extends Instruction_list {
    public Loop(String ref, Instruction[] elements) {
        super(InstructionName.LOOP, ref, elements);
    }
}
