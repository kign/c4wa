package net.inet_lab.c4wa.wat;

public class Block extends Instruction_list {
    public Block(String ref, Instruction[] elements) {
        super(InstructionName.BLOCK, ref, elements);
    }
}
