package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Const extends Expression {
    public final long longValue;
    public final double doubleValue;

    public Const(int value) {
        super(InstructionName.CONST, NumType.I32);
        longValue = value;
        doubleValue = 0;
    }

    public Const(long value) {
        super(InstructionName.CONST, NumType.I64);
        longValue = value;
        doubleValue = 0;
    }

    public Const(double value) {
        super(InstructionName.CONST, NumType.F64);
        longValue = 0;
        doubleValue = value;
    }

    public Const(NumType numType1, long value) {
        super(InstructionName.CONST, numType1);
        longValue = numType1 == NumType.I32?(int)value: numType1 == NumType.I64 ? value : 0;
        doubleValue = numType1 == NumType.I32 || numType1 == NumType.I64? 0 : value;
    }

    public Const(NumType numType, double value) {
        super(InstructionName.CONST, numType);
        if (numType == NumType.F32 || numType == NumType.F64) {
            longValue = 0;
            doubleValue = value;
        }
        else
            throw new RuntimeException("You can't do it");
    }

    public boolean isTrue() {
        return numType == NumType.I32 || numType == NumType.I64
                ? longValue != 0
                : doubleValue != 0;
    }

    public boolean isZero() {
        return is_int() && longValue == 0;
    }

    public boolean is_int() {
        return numType == NumType.I32 || numType == NumType.I64;
    }

    interface TwoArgIntOperator {
        long op(long a, long b);
    }

    interface TwoArgFloatOperator {
        double op(double a, double b);
    }

    @Override
    public String toString() {
        if (numType == NumType.I32 || numType == NumType.I64)
            return "(" + fullName() + " " + longValue + ")";
        else
            return "(" + fullName() + " " + doubleValue + ")";
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        out.writeDirect(opcode());
        if (numType == NumType.I32)
            out.writeSignedInt((int)longValue);
        else if (numType == NumType.I64)
            out.writeSignedLong(longValue);
        else if (numType == NumType.F32)
            out.writeFloat((float)doubleValue);
        else if (numType == NumType.F64)
            out.writeDouble(doubleValue);
        else
            throw new RuntimeException("invalid numType =" + numType);
    }

    int asInt() {
        assert numType == NumType.I32;
        return (int)longValue;
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return this;
    }
}
