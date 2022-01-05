package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Expression_0 extends Expression {
    Expression_0(InstructionName name) {
        super(name, null);
    }

    @Override
    public String toString() {
        return "(" + name.getName() + ")";
    }

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        out.writeOpcode(this);
        if (name == InstructionName.MEMORY_SIZE)
            out.writeDirect((byte) 0x00); // "memory.size reserved value must be 0"
    }
}
