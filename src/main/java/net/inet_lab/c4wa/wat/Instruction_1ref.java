package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Instruction_1ref extends Instruction {
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
    public Instruction[] postprocess(PostprocessContext ppctx) {
        if (arg == null)
            return new Instruction[]{this};
        else
            return new Instruction[]{new Instruction_1ref(type, ref, arg.postprocess(ppctx))};
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
        if (type == InstructionName.SET_GLOBAL)
            out.writeUnsignedInt(mCtx.globals.get(ref));
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
