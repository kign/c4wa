package net.inet_lab.c4wa.wat;

abstract public class Instruction_ref extends Instruction {
    public final String ref;
    public Instruction_ref(InstructionType type, String ref) {
        super(type);
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + ")";
    }
}
