package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Expression_3 extends Expression {
    final Expression arg1;
    final Expression arg2;
    final Expression arg3;

    Expression_3(InstructionName name, NumType numType, Expression arg1, Expression arg2, Expression arg3) {
        super(name, numType);
        this.arg1 = arg1.comptime_eval();
        this.arg2 = arg2.comptime_eval();
        this.arg3 = arg3.comptime_eval();
    }

    @Override
    public int complexity() {
        return 1 + Math.max(Math.max(arg1.complexity(), arg2.complexity()), arg3.complexity());
    }

    @Override
    public String toString() {
        return "(" + fullName() + " " + arg1 + " " + arg2 + " " + arg3 + ")";
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg1.wasm(mCtx, fCtx, out);
        arg2.wasm(mCtx, fCtx, out);
        arg3.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
    }
}
