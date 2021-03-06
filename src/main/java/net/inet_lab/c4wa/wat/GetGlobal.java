package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class GetGlobal extends Expression_ref {
    public GetGlobal(NumType numType, String ref) {
        super(InstructionName.GET_GLOBAL, numType, ref);
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        out.writeOpcode(this);
        out.writeUnsignedInt(mCtx.globals.get(ref));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return ectx.getGlobal(ref);
    }
}
