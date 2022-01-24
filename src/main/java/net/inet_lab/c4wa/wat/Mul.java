package net.inet_lab.c4wa.wat;

public class Mul extends Expression_2 {
    public Mul(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.MUL, numType, arg1, arg2, (a,b)->a*b, (a,b)->a*b);
    }

    @Override
    public Expression comptime_eval() {
        Const a1 = null;
        Expression a2 = null;
        if (arg1 instanceof Const) {
            a1 = (Const) arg1;
            a2 = arg2;
        }
        else if (arg2 instanceof Const) {
            a1 = (Const) arg2;
            a2 = arg1;
        }

        if (a1 != null) {
            if (((a1.numType == NumType.I32 || a1.numType == NumType.I64) && a1.longValue == 1) ||
                    ((a1.numType == NumType.F32 || a1.numType == NumType.F64) && a1.doubleValue == 1.0))
                return a2;
            if (((a1.numType == NumType.I32 || a1.numType == NumType.I64) && a1.longValue == 0) ||
                    ((a1.numType == NumType.F32 || a1.numType == NumType.F64) && a1.doubleValue == 0.0))
                return new Const(numType, 0);
        }
        return super.comptime_eval();
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Mul(numType, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
