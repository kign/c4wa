package net.inet_lab.c4wa.wat;

public class Sqrt extends Expression_1 {
    public Sqrt(NumType numType, Expression arg) {
        super(InstructionName.SQRT, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Sqrt(numType, arg.postprocess(ppctx));
    }
}
