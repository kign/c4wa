package net.inet_lab.c4wa.wat;

public class Loop extends Instruction_list {
    public final String ref;
    public Loop(String ref, Instruction[] elements) {
        super(InstructionName.LOOP, elements);
        this.ref = ref;
    }

    @Override
    String detailsToString() {
        return "$" + ref;
    }
}
