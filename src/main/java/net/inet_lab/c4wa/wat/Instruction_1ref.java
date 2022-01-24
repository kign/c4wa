package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Instruction_1ref extends Instruction {
    public final String ref;
    final public Expression arg;

    public Instruction_1ref(InstructionType type, String ref, Expression arg) {
        super(type);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + arg +")";
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
        if (type == InstructionName.SET_GLOBAL) {
            Integer global_idx = mCtx.globals.get(ref);
            if (global_idx == null)
                throw new RuntimeException("Global variable '$" + ref + "' not found" );
            out.writeUnsignedInt(global_idx);
        }
        else if (type == InstructionName.SET_LOCAL)
            out.writeUnsignedInt(fCtx.locals.get(ref));
        else if (type == InstructionName.BR_IF) {
            int idx = 0;
            for (String blockId: fCtx.blockStack) {
                if (blockId.equals(ref))
                    break;
                idx++;
            }
            if (idx == fCtx.blockStack.size())
                throw new RuntimeException("Cannot find block label " + ref);
            out.writeUnsignedInt(idx);
        }
    }
}
