package net.inet_lab.c4wa.wat;

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

    public Const(float value) {
        super(InstructionName.CONST, NumType.F32);
        longValue = 0;
        doubleValue = value;
    }

    public Const(double value) {
        super(InstructionName.CONST, NumType.F64);
        longValue = 0;
        doubleValue = value;
    }

    public Const(NumType numType1, long value) {
        super(InstructionName.CONST, numType1);
        longValue = (numType1 == NumType.I32 || numType1 == NumType.I64) ? value : 0;
        doubleValue = (numType1 == NumType.I32 || numType1 == NumType.I64)? 0 : value;
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

    public Const(NumType numType, Const orig) {
        super(InstructionName.CONST, numType);
        boolean s_int = orig.numType == NumType.I32 || orig.numType == NumType.I64;
        boolean d_int = numType == NumType.I32 || numType == NumType.I64;
        longValue =  d_int? (s_int? orig.longValue : (long) orig.doubleValue) : 0;
        doubleValue = d_int? 0 : (s_int? (double)orig.longValue : orig.doubleValue);
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
}
