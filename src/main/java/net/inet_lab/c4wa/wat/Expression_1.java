package net.inet_lab.c4wa.wat;

import java.io.IOException;

public abstract class Expression_1 extends Expression {
    final Expression arg;
    Expression_1(InstructionName name, NumType numType, Expression arg) {
        super(name, numType);
        this.arg = arg.comptime_eval();
    }

    @Override
    public int complexity() {
        return 1 + arg.complexity();
    }

    @Override
    public String toString() {
        return "(" + fullName() + " " + arg + ")";
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
        if (name == InstructionName.MEMORY_GROW)
            out.writeDirect((byte) 0x00); // "memory.size reserved value must be 0"
    }

    Const evalConst(Const val) {
        return null;
    }

    @Override
    public Expression comptime_eval() {
        if (arg instanceof Const) {
            Const res = evalConst((Const) arg);
            if (res != null)
                return res;
        }
        return this;
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        Const res = evalConst(arg.eval(ectx));
        if (res == null)
            throw new RuntimeException("evalConst not defined for " + fullName());
        return res;
    }
}
