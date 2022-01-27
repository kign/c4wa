package net.inet_lab.c4wa.wat;

public class Popcnt extends Expression_1 {
    public Popcnt(NumType numType, Expression arg) {
        super(InstructionName.POPCNT, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Popcnt(numType, arg.postprocess(ppctx));
    }

    @Override
    public Const evalConst(Const val) {
        assert numType.is_int();
        if (numType == NumType.I32)
            return new Const(numType, Integer.bitCount((int) val.longValue));
        else
            return new Const(numType, Long.bitCount(val.longValue));
    }
}
