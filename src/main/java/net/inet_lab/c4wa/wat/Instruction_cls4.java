package net.inet_lab.c4wa.wat;

public class Instruction_cls4 extends Instruction {
    public final String ref;
    final public Instruction arg;

    public Instruction_cls4(InstructionType type, String ref, Instruction arg) {
        super(type);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + arg +")";
    }
}
