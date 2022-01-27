package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class GetLocal extends Expression_ref {
    public GetLocal(NumType numType, String ref) {
        super(InstructionName.GET_LOCAL, numType, ref);
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        out.writeOpcode(this);
        out.writeUnsignedInt(fCtx.locals.get(ref));
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return ectx.getCurrentFunc().getLocal(ref);
    }
}
