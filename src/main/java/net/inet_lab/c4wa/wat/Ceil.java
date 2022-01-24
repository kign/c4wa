package net.inet_lab.c4wa.wat;

public class Ceil extends Expression_1 {
    public Ceil(NumType numType, Expression arg) {
        super(InstructionName.CEIL, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Ceil(numType, arg.postprocess(ppctx));
    }
}
