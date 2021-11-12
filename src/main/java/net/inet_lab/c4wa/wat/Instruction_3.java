package net.inet_lab.c4wa.wat;

abstract public class Instruction_3 extends Instruction {
    final public Instruction arg1;
    final public Instruction arg2;
    final public Instruction arg3;

    public Instruction_3(InstructionType type, Instruction arg1, Instruction arg2, Instruction arg3) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + " " + arg3 + ")";
    }
}
