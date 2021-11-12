package net.inet_lab.c4wa.wat;

public class Cmp extends Instruction_Bin {
    public Cmp(NumType numType, boolean bLess, boolean bEqual, boolean bSigned, Instruction lhs, Instruction rhs) {
        super(new InstructionWithNumPrefix(numType,
                (numType == NumType.F32 || numType == NumType.F64)?(
                        bLess?(bEqual?InstructionName.LE:InstructionName.LT):(bEqual ? InstructionName.GE : InstructionName.GT)):
                        (bLess ? (bEqual ? (bSigned?InstructionName.LE_S: InstructionName.LE_U) : (bSigned?InstructionName.LT_S: InstructionName.LT_U)) :
                                        (bEqual ? (bSigned ?InstructionName.GE_S: InstructionName.GE_U) : (bSigned ?InstructionName.GT_S: InstructionName.GT_U)))),
                lhs, rhs, null, null);
    }

    public Cmp(NumType numType, boolean bEqual, Instruction lhs, Instruction rhs) {
        super(new InstructionWithNumPrefix(numType, bEqual?InstructionName.EQ:InstructionName.NE), lhs, rhs, null, null);
    }

    public Cmp(Cmp o) {
        super(new InstructionWithNumPrefix(o.type.getPrefix(),
                (o.type.getMain() == InstructionName.LE)? InstructionName.GT :
                ((o.type.getMain() == InstructionName.LT)? InstructionName.GE :
                ((o.type.getMain() == InstructionName.GE) ? InstructionName.LT :
                ((o.type.getMain() == InstructionName.GT) ? InstructionName.LE :

                ((o.type.getMain() == InstructionName.LE_U)? InstructionName.GT_U :
                ((o.type.getMain() == InstructionName.LT_U)? InstructionName.GE_U :
                ((o.type.getMain() == InstructionName.GE_U) ? InstructionName.LT_U :
                ((o.type.getMain() == InstructionName.GT_U) ? InstructionName.LE_U :

                ((o.type.getMain() == InstructionName.LE_S)? InstructionName.GT_S :
                ((o.type.getMain() == InstructionName.LT_S)? InstructionName.GE_S :
                ((o.type.getMain() == InstructionName.GE_S) ? InstructionName.LT_S :
                ((o.type.getMain() == InstructionName.GT_S) ? InstructionName.LE_S :

                ((o.type.getMain() == InstructionName.EQ) ? InstructionName.NE :
                ((o.type.getMain() == InstructionName.NE) ? InstructionName.EQ :

                null
                )))))))))))))), o.arg1, o.arg2, null, null);
    }

    @Override
    public Instruction Not(NumType numType) {
        return new Cmp(this);
    }
}
