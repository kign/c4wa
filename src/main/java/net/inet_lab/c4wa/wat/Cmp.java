package net.inet_lab.c4wa.wat;

public class Cmp extends Expression_2 {
    public Cmp(NumType numType, boolean bLess, boolean bEqual, boolean bSigned, Expression lhs, Expression rhs) {
        super((numType == NumType.F32 || numType == NumType.F64)?(
                        bLess?(bEqual?InstructionName.LE:InstructionName.LT):(bEqual ? InstructionName.GE : InstructionName.GT)):
                        (bLess ? (bEqual ? (bSigned?InstructionName.LE_S: InstructionName.LE_U) : (bSigned?InstructionName.LT_S: InstructionName.LT_U)) :
                                        (bEqual ? (bSigned ?InstructionName.GE_S: InstructionName.GE_U) : (bSigned ?InstructionName.GT_S: InstructionName.GT_U))),
                numType, lhs, rhs, new IConstCmp(bLess, bEqual, bSigned), (a,b) -> (a == b)?(bEqual?1:0):a < b == bLess?1:0);
    }

    public Cmp(NumType numType, boolean bEqual, Expression lhs, Expression rhs) {
        super(bEqual?InstructionName.EQ:InstructionName.NE, numType, lhs, rhs, (a, b) -> (a==b) == bEqual? 1 : 0,
                (a, b) -> ((a == b) == bEqual)?1:0);
    }

    private Cmp(InstructionName name, NumType numType, Expression arg1, Expression arg2,
                        Const.TwoArgIntOperator op_i, Const.TwoArgFloatOperator op_f) {
        super(name, numType, arg1, arg2, op_i, op_f);
    }

    public Cmp(Cmp o) {
        super((o.name == InstructionName.LE)? InstructionName.GT :
                ((o.name == InstructionName.LT)? InstructionName.GE :
                ((o.name == InstructionName.GE) ? InstructionName.LT :
                ((o.name == InstructionName.GT) ? InstructionName.LE :

                ((o.name == InstructionName.LE_U)? InstructionName.GT_U :
                ((o.name == InstructionName.LT_U)? InstructionName.GE_U :
                ((o.name == InstructionName.GE_U) ? InstructionName.LT_U :
                ((o.name == InstructionName.GT_U) ? InstructionName.LE_U :

                ((o.name == InstructionName.LE_S)? InstructionName.GT_S :
                ((o.name == InstructionName.LT_S)? InstructionName.GE_S :
                ((o.name == InstructionName.GE_S) ? InstructionName.LT_S :
                ((o.name == InstructionName.GT_S) ? InstructionName.LE_S :

                ((o.name == InstructionName.EQ) ? InstructionName.NE :
                ((o.name == InstructionName.NE) ? InstructionName.EQ :

                null
                ))))))))))))), o.numType, o.arg1, o.arg2,
                                (a, b) -> o.op_i.op(a,b) == 0 ? 1 : 0,
                                (a, b) -> o.op_f.op(a, b) == 0 ? 1 : 0);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Cmp(name, numType, arg1.postprocess(ppctx), arg2.postprocess(ppctx), op_i, op_f);
    }

    private static class IConstCmp implements Const.TwoArgIntOperator {
        final boolean bLess;
        final boolean bEqual;
        final boolean bSigned;
        IConstCmp(boolean bLess, boolean bEqual, boolean bSigned) {
            this.bLess = bLess;
            this.bEqual = bEqual;
            this.bSigned = bSigned;
        }
        @Override
        public long op(long a, long b) {
            if (a == b)
                return bEqual? 1 : 0;

            long res = bSigned? Long.compare(a, b) : Long.compareUnsigned(a, b);
            return (res > 0) == !bLess ? 1 : 0;
        }
    }

    @Override
    public Expression Not(NumType numType) {
        return new Cmp(this);
    }
}
