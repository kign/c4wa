package net.inet_lab.c4wa.wat;

public class Clz extends Expression_1 {
    public Clz(NumType numType, Expression arg) {
        super(InstructionName.CLZ, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Clz(numType, arg.postprocess(ppctx));
    }

    @Override
    public Const evalConst(Const val) {
        assert numType.is_int();
        if (numType == NumType.I32)
            return new Const(numType, Integer.numberOfLeadingZeros((int)val.longValue));
        else
            return new Const(numType, Long.numberOfLeadingZeros(val.longValue));
    }
}
