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
}
