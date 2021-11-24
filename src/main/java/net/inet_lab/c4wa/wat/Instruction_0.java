package net.inet_lab.c4wa.wat;

public class Instruction_0 extends Instruction {
    public Instruction_0(InstructionType type) {
        super(type);
    }

    @Override
    public String toString() {
        return "(" + type.getName() + ")";
    }
}
