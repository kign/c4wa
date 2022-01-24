package net.inet_lab.c4wa.wat;

public class Ctz extends Expression_1 {
    public Ctz(NumType numType, Expression arg) {
        super(InstructionName.CTZ, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Ctz(numType, arg.postprocess(ppctx));
    }
}
