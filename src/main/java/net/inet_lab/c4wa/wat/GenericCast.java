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

        else
            throw new RuntimeException("Cast '" + srcType + "' => '" + dstType + "'" + (signed?"":" (unsigned)") +
                    "is not defined or not available");
        return new InstructionWithNumPrefix(dstType, name);
    }
}
