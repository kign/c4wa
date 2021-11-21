package net.inet_lab.c4wa.wat;

public class Instruction_2 extends Instruction {
    final public Expression arg1;
    final public Expression arg2;

    public Instruction_2(InstructionType type, Expression arg1, Expression arg2) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toStringPretty(int indent) {
        return toString();
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + ")";
    }
}
