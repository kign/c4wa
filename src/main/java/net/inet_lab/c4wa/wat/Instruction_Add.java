package net.inet_lab.c4wa.wat;

abstract public class Instruction_Add extends Instruction {
    final public Instruction arg1;
    final public Instruction arg2;

    public Instruction_Add(InstructionType type, Instruction arg1, Instruction arg2) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + ")";
    }
}
