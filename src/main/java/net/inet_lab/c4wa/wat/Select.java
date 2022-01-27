package net.inet_lab.c4wa.wat;

public class Select extends Expression_3 {
    public Select(Expression condition, Expression _then, Expression _else) {
        super(InstructionName.SELECT, null, _then, _else, condition);
    }

    @Override
    public Expression comptime_eval() {
        if (arg3 instanceof Const) {
            Const condition = (Const) arg3;

            if (condition.isTrue())
                return arg1;
            else
                return arg2;
        }
        else
            return this;
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Select(arg3.postprocess(ppctx), arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        int cond = arg3.eval(ectx).asInt();
        if (cond != 0)
            return arg1.eval(ectx);
        else
            return arg2.eval(ectx);
    }
}
