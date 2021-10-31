package net.inet_lab.c4wa.wat;

public abstract class Instruction {
    final public InstructionType type;

    public Instruction(InstructionType type) {
        this.type = type;
    }

    abstract public String toString();
}
