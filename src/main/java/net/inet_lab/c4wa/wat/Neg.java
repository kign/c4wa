package net.inet_lab.c4wa.wat;

public class Neg extends Expression_1 {
    public Neg(NumType numType, Expression arg) {
        super(InstructionName.NEG, numType, arg);
    }

    @Override
    public Expression comptime_eval() {
        if (arg instanceof Const)
            return new Const(numType, -((Const)arg).doubleValue);
        return this;
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Neg(numType, arg.postprocess(ppctx));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        Const a = arg.eval(ectx);
        assert numType.is_float();
        assert a.numType == numType;
        return new Const(numType, -a.doubleValue);
    }
}
