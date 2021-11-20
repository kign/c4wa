package net.inet_lab.c4wa.wat;

public class Instruction_Bin extends Instruction {
    final public Instruction arg1;
    final public Instruction arg2;
    final Const.TwoArgIntOperator op_i;
    final Const.TwoArgFloatOperator op_f;
    public Instruction_Bin(InstructionType type, Instruction arg1, Instruction arg2,
                           Const.TwoArgIntOperator op_i, Const.TwoArgFloatOperator op_f) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.op_i = op_i;
        this.op_f = op_f;
    }

    @Override
    public Instruction comptime_eval() {
        if (arg1 instanceof Const && arg2 instanceof Const) {
            Const a1 = (Const)arg1;
            Const a2 = (Const)arg2;

            if ((a1.numType == NumType.I32 || a1.numType == NumType.I64) && op_i != null)
                return new Const(a1.numType, op_i.op(a1.longValue, a2.longValue));
            if ((a1.numType == NumType.F32 || a1.numType == NumType.F64) && op_f != null)
                return new Const(a1.numType, op_f.op(a1.doubleValue, a2.doubleValue));
        }
        return this;
    }

    @Override
    public String toStringPretty(int indent) {
        return "(" + type.getName() + " " + arg1.toStringPretty(indent) + " " + arg2.toStringPretty(indent) + ")";
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + ")";
    }

    @Override
    public int complexity() {
        return 1 + Math.max(arg1.complexity(), arg2.complexity());
    }
}
