package net.inet_lab.c4wa.wat;

public class Sub extends Expression_2 {
    public Sub(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.SUB, numType, arg1, arg2, (a,b)->a-b, (a,b)->a-b);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Sub(numType, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
