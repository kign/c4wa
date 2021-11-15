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

    abstract public int complexity();

    public Instruction comptime_eval() {
        return this;
    }

    public Instruction Not(NumType numType) {
        if (numType == NumType.I32 || numType == NumType.I64)
            return new Eqz(numType, this);
        else
            throw new RuntimeException("Cannot take logical negative of '" + type.getName() + "'");
    }
}
