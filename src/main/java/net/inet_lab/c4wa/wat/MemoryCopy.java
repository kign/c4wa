package net.inet_lab.c4wa.wat;

public class MemoryCopy extends Instruction_3 {
    public MemoryCopy(Expression dest, Expression src, Expression size) {
        super(InstructionName.MEMORY_COPY, dest, src, size);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new MemoryCopy(arg1.postprocess(ppctx), arg2.postprocess(ppctx), arg3.postprocess(ppctx))};
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        ectx.memoryCopy(arg1.eval(ectx).asInt(), arg2.eval(ectx).asInt(), arg3.eval(ectx).asInt());
    }
}
