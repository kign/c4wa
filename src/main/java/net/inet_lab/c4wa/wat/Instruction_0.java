package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Instruction_0 extends Instruction {
    public Instruction_0(InstructionType type) {
        super(type);
    }

    @Override
    public String toString() {
        return "(" + type.getName() + ")";
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        out.writeOpcode(type);
    }
}
