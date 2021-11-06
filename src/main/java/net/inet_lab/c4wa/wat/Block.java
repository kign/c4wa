package net.inet_lab.c4wa.wat;

public class Block extends Instruction_list {
    public final String ref;

    public Block(String ref, Instruction[] elements) {
        super(InstructionName.BLOCK, elements);
        this.ref = ref;
    }

    @Override
    String detailsToString() {
        return "$" + ref;
    }
}
