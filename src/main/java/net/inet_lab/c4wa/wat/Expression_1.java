package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Expression_1 extends Expression {
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
    public Expression postprocess(PostprocessContext ppctx) {
        return new Expression_1(name, numType, arg.postprocess(ppctx));
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
    }
}
