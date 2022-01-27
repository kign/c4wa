package net.inet_lab.c4wa.wat;

public class Ctz extends Expression_1 {
    public Ctz(NumType numType, Expression arg) {
        super(InstructionName.CTZ, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Ctz(numType, arg.postprocess(ppctx));
    }

    @Override
    public Const evalConst(Const val) {
        assert numType.is_int();
        if (numType == NumType.I32)
            return new Const(numType, Integer.numberOfTrailingZeros((int) val.longValue));
        else
            return new Const(numType, Long.numberOfTrailingZeros(val.longValue));
    }
}
