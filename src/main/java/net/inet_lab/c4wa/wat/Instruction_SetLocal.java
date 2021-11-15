package net.inet_lab.c4wa.wat;

public class Instruction_SetLocal extends Instruction {
    public final String ref;
    final public Instruction arg;

    public Instruction_SetLocal(InstructionType type, String ref, Instruction arg) {
        super(type);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + arg +")";
    }

    @Override
    public int complexity() {
        return 0;
    }
}
