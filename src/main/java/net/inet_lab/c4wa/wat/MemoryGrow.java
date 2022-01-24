package net.inet_lab.c4wa.wat;

public class MemoryGrow extends Expression_1 {
    public MemoryGrow(Expression arg) {
        super(InstructionName.MEMORY_GROW, null, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new MemoryGrow(arg.postprocess(ppctx));
    }
}
