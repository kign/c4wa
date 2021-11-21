package net.inet_lab.c4wa.wat;

public abstract class Instruction {
    final public InstructionType type;

    public Instruction(InstructionType type) {
        this.type = type;
    }

    public String toStringPretty(int indent) {
        return toString();
    }

    abstract public String toString();

    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{this};
    }
}
