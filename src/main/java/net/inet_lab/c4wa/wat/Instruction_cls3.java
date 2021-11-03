package net.inet_lab.c4wa.wat;

// return
public class Instruction_cls3 extends Instruction {
    public final Instruction arg;

    public Instruction_cls3(InstructionType type, Instruction arg) {
        super(type);
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg + ")";
    }
}
