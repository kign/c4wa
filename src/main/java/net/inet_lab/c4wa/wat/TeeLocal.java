package net.inet_lab.c4wa.wat;

public class TeeLocal extends Expression_1ref {
    public TeeLocal(NumType numType, String ref, Expression arg) {
        super(InstructionName.TEE_LOCAL, numType, ref, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new TeeLocal(numType, ref, arg.postprocess(ppctx));
    }
}
