package net.inet_lab.c4wa.wat;

abstract public class Instruction_GetLocal extends Instruction {
    public final String ref;
    public Instruction_GetLocal(InstructionType type, String ref) {
        super(type);
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + ")";
    }
}