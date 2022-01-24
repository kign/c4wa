package net.inet_lab.c4wa.wat;

public class Abs extends Expression_1 {
    public Abs(NumType numType, Expression arg) {
        super(InstructionName.ABS, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Abs(numType, arg.postprocess(ppctx));
    }
}
