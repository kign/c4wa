package net.inet_lab.c4wa.wat;

public class Cmp extends Expression_2 {
    public Cmp(NumType numType, boolean bLess, boolean bEqual, boolean bSigned, Expression lhs, Expression rhs) {
        super((numType == NumType.F32 || numType == NumType.F64)?(
                        bLess?(bEqual?InstructionName.LE:InstructionName.LT):(bEqual ? InstructionName.GE : InstructionName.GT)):
                        (bLess ? (bEqual ? (bSigned?InstructionName.LE_S: InstructionName.LE_U) : (bSigned?InstructionName.LT_S: InstructionName.LT_U)) :
                                        (bEqual ? (bSigned ?InstructionName.GE_S: InstructionName.GE_U) : (bSigned ?InstructionName.GT_S: InstructionName.GT_U))),
                numType, lhs, rhs, null, null);
    }

    public Cmp(NumType numType, boolean bEqual, Expression lhs, Expression rhs) {
        super(bEqual?InstructionName.EQ:InstructionName.NE, numType, lhs, rhs, null, null);
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

    @Override
    public Expression Not(NumType numType) {
        return new Cmp(this);
    }
}
