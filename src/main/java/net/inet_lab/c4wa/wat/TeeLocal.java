package net.inet_lab.c4wa.wat;

public class TeeLocal extends Expression_1ref {
    public TeeLocal(NumType numType, String ref, Expression arg) {
        super(InstructionName.TEE_LOCAL, numType, ref, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new TeeLocal(numType, ref, arg.postprocess(ppctx));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        ExecutionFunc f = ectx.getCurrentFunc();
        Const res = arg.eval(ectx);
        f.assignLocal(ref, res);

        return res;
    }
}
