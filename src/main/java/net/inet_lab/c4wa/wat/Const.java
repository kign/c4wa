package net.inet_lab.c4wa.wat;

public class Const extends Instruction {
    final NumType numType;
    final long longValue;
    final double doubleValue;

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
        super(new InstructionWithNumPrefix(NumType.I64, InstructionName.CONST));
        numType = NumType.F32;
        longValue = 0;
        doubleValue = value;
    }

    public Const(double value) {
        super(new InstructionWithNumPrefix(NumType.I64, InstructionName.CONST));
        numType = NumType.F64;
        longValue = 0;
        doubleValue = value;
    }

    @Override
    public String toString() {
        if (numType == NumType.I32 || numType == NumType.I64)
            return "(" + type.getName() + " " + longValue + ")";
        else
            return "(" + type.getName() + " " + doubleValue + ")";
    }
}
