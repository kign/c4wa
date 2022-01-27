package net.inet_lab.c4wa.wat;

import org.antlr.v4.runtime.atn.PredicateTransition;

public class BrIfExp extends Expression_2ref {
    public BrIfExp(String ref, NumType numType, Expression condition, Expression returnValue) {
        super(InstructionName.BR_IF, numType, ref, returnValue, condition);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new BrIfExp(ref, numType, arg2.postprocess(ppctx), arg1.postprocess(ppctx));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        // current compiler implementation always DROP the result, but I believe
        // BR_IF technically returns its operand; thus it is ALWAYS evaluated
        Const ret = arg1.eval(ectx);
        int cond = arg2.eval(ectx).asInt();
        if (cond != 0)
            throw new ExecutionFunc.ExeBreak(ref, ret);
        else
            return ret;
    }
}
