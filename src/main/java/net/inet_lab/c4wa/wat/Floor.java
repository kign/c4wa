package net.inet_lab.c4wa.wat;

public class Floor extends Expression_1 {
    public Floor(NumType numType, Expression arg) {
        super(InstructionName.FLOOR, numType, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Floor(numType, arg.postprocess(ppctx));
    }

    @Override
    Const evalConst(Const val) {
        assert val.numType.is_float();
        return new Const(numType, (long) Math.floor(val.doubleValue));
    }
}
