package net.inet_lab.c4wa.wat;

public class MemoryGrow extends Instruction_1 {
    public MemoryGrow(Expression arg) {
        super(InstructionName.MEMORY_GROW, arg);
    }
}
