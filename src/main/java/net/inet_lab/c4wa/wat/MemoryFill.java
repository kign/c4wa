package net.inet_lab.c4wa.wat;

public class MemoryFill extends Instruction_3 {
    public MemoryFill(Expression dest, Expression value, Expression size) {
        super(InstructionName.MEMORY_FILL, dest, value, size);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new MemoryFill(arg1.postprocess(ppctx), arg2.postprocess(ppctx), arg3.postprocess(ppctx))};
    }
}
