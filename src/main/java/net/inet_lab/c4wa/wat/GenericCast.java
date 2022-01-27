package net.inet_lab.c4wa.wat;

public class GenericCast extends Expression_1 {
    private GenericCast(NumType srcType, NumType dstType, boolean signed, Expression arg) {
        super(from_to(srcType, dstType, signed), dstType, arg);
    }

    private GenericCast(InstructionName name, NumType dstType, Expression arg) {
        super(name, dstType, arg);
    }

    static private InstructionName from_to(NumType srcType, NumType dstType, boolean signed) {
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
        return name;
    }

    static public Expression cast(NumType srcType, NumType dstType, boolean signed, Expression arg) {
        if (dstType == srcType)
            return arg;
//        else if (arg instanceof Const)
//            return new Const(dstType, (Const)arg, signed);
        else
            return new GenericCast(srcType, dstType, signed, arg);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new GenericCast(name, numType, arg.postprocess(ppctx));
    }

    @Override
    Const evalConst(Const val) {
        switch (name) {
            case WRAP_I64:
                assert val.numType == NumType.I64;
                return new Const((int) val.longValue);
            case CONVERT_I32_S:
                assert val.numType == NumType.I32;
                assert numType == NumType.F32 || numType == NumType.F64;
                return new Const(numType, (double) val.asInt());
            case CONVERT_I32_U:
                assert val.numType == NumType.I32;
                assert numType == NumType.F32 || numType == NumType.F64;
                double res = val.asInt();
                return new Const(numType, res < 0 ? res + Math.pow(2, 32) : res);
            case CONVERT_I64_S:
                assert val.numType == NumType.I64;
                assert numType == NumType.F32 || numType == NumType.F64;
                return new Const(numType, (double) val.longValue);
            case CONVERT_I64_U:
                assert val.numType == NumType.I64;
                assert numType == NumType.F32 || numType == NumType.F64;
                return new Const(numType, (double) val.longValue + Math.pow(2, 64));
            case EXTEND_I32_S:
                assert val.numType == NumType.I32;
                return new Const((long) val.asInt());
            case EXTEND_I32_U:
                assert val.numType == NumType.I32;
                return new Const(Integer.toUnsignedLong(val.asInt()));
            case TRUNC_F64_S:
            case TRUNC_F64_U:
                // FIXME: Obviously something must be wrong here
                assert val.numType == NumType.F64;
                assert numType.is_int();
                return new Const(numType, (long)val.doubleValue);
            case PROMOTE_F32:
                assert val.numType == NumType.F32;
                return new Const(numType, val.doubleValue);
            case DEMOTE_F64:
                assert val.numType == NumType.F64;
                return new Const(numType, val.doubleValue);
            default:
                throw new RuntimeException("name = " + name + " not yet covered");
        }
    }

/*
    @Override
    public Expression comptime_eval() {
        if (arg instanceof Const)
            return evalConst((Const)arg);
        return this;
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return evalConst(arg.eval(ectx));
    }
*/
}
