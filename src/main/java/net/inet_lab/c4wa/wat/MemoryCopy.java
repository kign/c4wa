package net.inet_lab.c4wa.wat;

public class MemoryCopy extends Instruction_3 {
    public MemoryCopy(Expression dest, Expression value, Expression size) {
        super(InstructionName.MEMORY_COPY, dest, value, size);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new MemoryCopy(arg1.postprocess(ppctx), arg2.postprocess(ppctx), arg3.postprocess(ppctx))};
    }
}
