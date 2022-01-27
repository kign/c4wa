package net.inet_lab.c4wa.wat;

public class MemoryGrow extends Expression_1 {
    public MemoryGrow(Expression arg) {
        super(InstructionName.MEMORY_GROW, null, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new MemoryGrow(arg.postprocess(ppctx));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        int delta = arg.eval(ectx).asInt();
        int new_pages = ectx.memoryGrow(delta);
        return new Const(NumType.I32, new_pages);
    }
}
