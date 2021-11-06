package net.inet_lab.c4wa.wat;

public class Instruction_Return extends Instruction {
    public final Instruction arg;

    public Instruction_Return(InstructionType type, Instruction arg) {
        super(type);
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg + ")";
    }
}
