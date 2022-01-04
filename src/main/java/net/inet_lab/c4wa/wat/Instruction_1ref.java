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
        out.writeInt(type.getMain() == InstructionName.SET_GLOBAL? mCtx.globals.get(ref) : fCtx.locals.get(ref));
    }
}
