package net.inet_lab.c4wa.wat;

public class Cmp extends Expression_2 {
    public Cmp(NumType numType, boolean bLess, boolean bEqual, boolean bSigned, Expression lhs, Expression rhs) {
        super((numType == NumType.F32 || numType == NumType.F64)?(
                        bLess?(bEqual?InstructionName.LE:InstructionName.LT):(bEqual ? InstructionName.GE : InstructionName.GT)):
                        (bLess ? (bEqual ? (bSigned?InstructionName.LE_S: InstructionName.LE_U) : (bSigned?InstructionName.LT_S: InstructionName.LT_U)) :
                                        (bEqual ? (bSigned ?InstructionName.GE_S: InstructionName.GE_U) : (bSigned ?InstructionName.GT_S: InstructionName.GT_U))),
                numType, lhs, rhs, new IConstCmp(bLess, bEqual, bSigned), null);
    }

    public Cmp(NumType numType, boolean bEqual, Expression lhs, Expression rhs) {
        super(bEqual?InstructionName.EQ:InstructionName.NE, numType, lhs, rhs, (a, b) -> (a==b) == bEqual? 1 : 0, null);
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
                ))))))))))))), o.numType, o.arg1, o.arg2, null, null);
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
            if (bEqual && a == b)
                return 1;

            long res = bSigned? Long.compare(a, b) : Long.compareUnsigned(a, b);
            return (res > 0) == !bLess ? 1 : 0;
        }
    }

    @Override
    public Expression Not(NumType numType) {
        return new Cmp(this);
    }
}
