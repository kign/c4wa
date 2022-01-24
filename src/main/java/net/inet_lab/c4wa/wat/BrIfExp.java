package net.inet_lab.c4wa.wat;

public class BrIfExp extends Expression_2ref {
    public BrIfExp(String ref, NumType numType, Expression condition, Expression returnValue) {
        super(InstructionName.BR_IF, numType, ref, returnValue, condition);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new BrIfExp(ref, numType, arg2.postprocess(ppctx), arg1.postprocess(ppctx));
    }
}
