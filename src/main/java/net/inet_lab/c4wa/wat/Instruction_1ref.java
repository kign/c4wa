package net.inet_lab.c4wa.wat;

public class Instruction_1ref extends Instruction {
    public final String ref;
    final public Expression arg;

    public Instruction_1ref(InstructionType type, String ref, Expression arg) {
        super(type);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + arg +")";
    }
}
