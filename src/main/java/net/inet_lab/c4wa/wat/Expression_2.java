package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Expression_2 extends Expression {
    final public Expression arg1;
    final public Expression arg2;
    final Const.TwoArgIntOperator op_i;
    final Const.TwoArgFloatOperator op_f;
    public Expression_2(InstructionName name, NumType numType, Expression arg1, Expression arg2,
                        Const.TwoArgIntOperator op_i, Const.TwoArgFloatOperator op_f) {
        super(name, numType);
        this.arg1 = arg1.comptime_eval();
        this.arg2 = arg2.comptime_eval();
        this.op_i = op_i;
        this.op_f = op_f;
    }

    private Const comptime_eval(Const a1, Const a2) {
        if (a1.numType != a2.numType)
            throw new RuntimeException("Operation " + fullName() + ", operands have types " + a1.numType + " and " + a2.numType);
        if (numType != null && a1.numType != numType)
            throw new RuntimeException("Operation " + fullName() + ", operands have types " + a1.numType);

        boolean is_boolean = this instanceof Cmp;
        if (a1.is_int()) {
            if (op_i == null)
                throw new RuntimeException("Operation " + fullName() + ", no integer evaluator");
            long res = op_i.op(a1.longValue, a2.longValue);
            if (is_boolean)
                return new Const(NumType.I32, res != 0? 1 : 0);
            else
                return new Const(a1.numType, res);
        }
        else {
            if (op_f == null)
                throw new RuntimeException("Operation " + fullName() + ", no float evaluator");
            double res = op_f.op(a1.doubleValue, a2.doubleValue);
            if (is_boolean)
                return new Const(NumType.I32, res > 0.5? 1 : 0);
            else
                return new Const(a1.numType, res);
        }
    }

    @Override
    public Expression comptime_eval() {
        if (arg1 instanceof Const && arg2 instanceof Const)
            return comptime_eval((Const) arg1, (Const) arg2);

        return this;
    }

    @Override
    public String toString() {
        return "(" + fullName() + " " + arg1 + " " + arg2 + ")";
    }

    @Override
    public int complexity() {
        return 1 + Math.max(arg1.complexity(), arg2.complexity());
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg1.wasm(mCtx, fCtx, out);
        arg2.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return comptime_eval(arg1.eval(ectx), arg2.eval(ectx));
    }

}
