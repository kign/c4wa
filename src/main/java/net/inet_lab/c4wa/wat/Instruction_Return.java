package net.inet_lab.c4wa.wat;

public class Instruction_Return extends Instruction {
    public final Instruction arg;

    public Instruction_Return(InstructionType type, Instruction arg) {
        super(type);
        this.arg = arg;
    }

    public String toStringPretty(int indent) {
        if (arg == null)
            return "(" + type.getName() + ")";
        else
            return "(" + type.getName() + " " + arg.toStringPretty(indent) + ")";
    }

    @Override
    public String toString() {
        if (arg == null)
            return "(" + type.getName() + ")";
        else
            return "(" + type.getName() + " " + arg + ")";
    }

    @Override
    public int complexity() {
        if (arg == null)
            return 0;
        else
            return arg.complexity();
    }
}
