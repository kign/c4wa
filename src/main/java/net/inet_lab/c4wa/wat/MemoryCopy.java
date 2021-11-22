package net.inet_lab.c4wa.wat;

public class MemoryCopy extends Instruction_3 {
    public MemoryCopy(Expression dest, Expression value, Expression size) {
        super(InstructionName.MEMORY_COPY, dest, value, size);
    }
}
