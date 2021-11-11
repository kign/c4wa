package net.inet_lab.c4wa.wat;

public class GenericCast extends Instruction_Return {
    public GenericCast(NumType srcType, NumType dstType, boolean signed, Instruction arg) {
        super(from_to(srcType, dstType, signed), arg);
    }

    static private InstructionType from_to(NumType srcType, NumType dstType, boolean signed) {
        InstructionName name;

        if (srcType == NumType.I64 && dstType == NumType.I32)
            name = InstructionName.WRAP_I64;
        else if (srcType == NumType.I32 && dstType == NumType.I64)
            name = signed? InstructionName.EXTEND_I32_S: InstructionName.EXTEND_I32_U;
        else if (srcType == NumType.I32 && (dstType == NumType.F32 || dstType == NumType.F64))
            name = signed? InstructionName.CONVERT_I32_S: InstructionName.CONVERT_I32_U;
        else if (srcType == NumType.I64 && (dstType == NumType.F32 || dstType == NumType.F64))
            name = signed? InstructionName.CONVERT_I64_S: InstructionName.CONVERT_I64_U;
        else if (srcType == NumType.F64 && dstType == NumType.F32)
            name = InstructionName.DEMOTE_F64;
        else if (srcType == NumType.F32 && dstType == NumType.F64)
            name = InstructionName.PROMOTE_F32;
        else if (srcType == NumType.F32 && (dstType == NumType.I32 || dstType == NumType.I64))
            name = signed? InstructionName.TRUNC_F32_S: InstructionName.TRUNC_F32_U;
        else if (srcType == NumType.F64 && (dstType == NumType.I32 || dstType == NumType.I64))
            name = signed? InstructionName.TRUNC_F64_S: InstructionName.TRUNC_F64_U;

        else
            throw new RuntimeException("Cast '" + srcType + "' => '" + dstType + "'" + (signed?"":" (unsigned)") +
                    "is not defined or not available");
        return new InstructionWithNumPrefix(dstType, name);
    }

    static public Instruction cast(NumType srcType, NumType dstType, boolean signed, Instruction arg) {
        if (arg instanceof Const)
            return new Const(dstType, (Const)arg);
        else
            return new GenericCast(srcType, dstType, signed, arg);
    }
}
