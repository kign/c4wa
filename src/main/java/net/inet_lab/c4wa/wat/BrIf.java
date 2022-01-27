package net.inet_lab.c4wa.wat;

public class BrIf extends Instruction_1ref {
    public BrIf(String ref, Expression condition) {
        super(InstructionName.BR_IF, ref, condition);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new BrIf(ref, arg.postprocess(ppctx))};
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        int cond = arg.eval(ectx).asInt();
        if (cond != 0)
            throw new ExecutionFunc.ExeBreak(ref);
    }
}
