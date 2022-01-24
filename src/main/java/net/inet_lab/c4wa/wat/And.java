package net.inet_lab.c4wa.wat;

public class And extends Expression_2 {
    public And(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.AND, numType, arg1, arg2, (a,b) -> a&b, null);
    }

    @Override
    public Expression Not(NumType numType) {
        return new Or(numType, arg1.Not(numType), arg2.Not(numType));
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new And(numType, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
