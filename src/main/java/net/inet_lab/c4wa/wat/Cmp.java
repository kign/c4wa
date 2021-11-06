package net.inet_lab.c4wa.wat;

public class Cmp extends Instruction_Add {
    public Cmp(NumType numType, boolean bLess, boolean bEqual, boolean bSigned, Instruction lhs, Instruction rhs) {
        super(new InstructionWithNumPrefix(numType,
                (numType == NumType.F32 || numType == NumType.F64)?(
                        bLess?(bEqual?InstructionName.LE:InstructionName.LT):(bEqual ? InstructionName.GE : InstructionName.GT)):
                        (bLess ? (bEqual ? (bSigned?InstructionName.LE_S: InstructionName.LE_U) : (bSigned?InstructionName.LT_S: InstructionName.LT_U)) :
                                        (bEqual ? (bSigned ?InstructionName.GE_S: InstructionName.GE_U) : (bSigned ?InstructionName.GT_S: InstructionName.GT_U)))),
                lhs, rhs);
    }
}
