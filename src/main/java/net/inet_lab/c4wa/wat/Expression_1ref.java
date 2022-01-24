package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Expression_1ref extends Expression {
    final String ref;
    final Expression arg;

    public Expression_1ref(InstructionName name, NumType numType, String ref, Expression arg) {
        super(name, numType);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + name.getName() + " $" + ref + " " + arg + ")";
    }

    @Override
    public int complexity() {
        return 1 + arg.complexity();
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
        out.writeUnsignedInt(fCtx.locals.get(ref));
    }
}

