package net.inet_lab.c4wa.wat;

public class Instruction_Return extends Instruction {
    public final Instruction arg;

    public Instruction_Return(InstructionType type, Instruction arg) {
        super(type);
        this.arg = arg;
    }

    public String toStringPretty(int indent) {
        return "(" + type.getName() + " " + arg.toStringPretty(indent) + ")";
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg + ")";
    }

    @Override
    public int complexity() {
        return arg.complexity();
    }
}
