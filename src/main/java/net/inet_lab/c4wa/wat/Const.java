package net.inet_lab.c4wa.wat;

public class Const extends Instruction {
    public final NumType numType;
    public final long longValue;
    public final double doubleValue;

    public Const(int value) {
        super(new InstructionWithNumPrefix(NumType.I32, InstructionName.CONST));
        numType = NumType.I32;
        longValue = value;
        doubleValue = 0;
    }

    public Const(long value) {
        super(new InstructionWithNumPrefix(NumType.I64, InstructionName.CONST));
        numType = NumType.I64;
        longValue = value;
        doubleValue = 0;
    }

    public Const(float value) {
        super(new InstructionWithNumPrefix(NumType.F32, InstructionName.CONST));
        numType = NumType.F32;
        longValue = 0;
        doubleValue = value;
    }

    public Const(double value) {
        super(new InstructionWithNumPrefix(NumType.F64, InstructionName.CONST));
        numType = NumType.F64;
        longValue = 0;
        doubleValue = value;
    }

    public Const(NumType numType, long value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.CONST));
        this.numType = numType;
        longValue = (numType == NumType.I32 || numType == NumType.I64) ? value : 0;
        doubleValue = (numType == NumType.I32 || numType == NumType.I64)? 0 : value;
    }

    public Const(NumType numType, double value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.CONST));
        this.numType = numType;
        if (numType == NumType.F32 || numType == NumType.F64) {
            longValue = 0;
            doubleValue = value;
        }
        else
            throw new RuntimeException("You can't do it");
    }

    public Const(NumType numType, Const orig) {
        super(new InstructionWithNumPrefix(numType, InstructionName.CONST));
        boolean s_int = orig.numType == NumType.I32 || orig.numType == NumType.I64;
        boolean d_int = numType == NumType.I32 || numType == NumType.I64;
        this.numType = numType;
        longValue =  d_int? (s_int? orig.longValue : (long) orig.doubleValue) : 0;
        doubleValue = d_int? 0 : (s_int? (double)orig.longValue : orig.doubleValue);
    }

    public boolean isTrue() {
        return numType == NumType.I32 || numType == NumType.I64
                ? longValue != 0
                : doubleValue != 0;
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
            return "(" + type.getName() + " " + longValue + ")";
        else
            return "(" + type.getName() + " " + doubleValue + ")";
    }

    @Override
    public int complexity() {
        return 0;
    }
}
