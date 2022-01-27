package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Br extends Instruction_ref {
    public Br(String ref) {
        super(InstructionName.BR, ref);
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        int idx = 0;
        for (String blockId : fCtx.blockStack) {
            if (blockId.equals(ref))
                break;
            idx++;
        }
        if (idx == fCtx.blockStack.size())
            throw new RuntimeException("Cannot find block label " + ref);
        out.writeOpcode(type);
        out.writeUnsignedInt(idx);
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        throw new ExecutionFunc.ExeBreak(ref);
    }
}
