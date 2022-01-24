package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Instruction_1 extends Instruction {
    public final Expression arg;

    public Instruction_1(InstructionType type, Expression arg) {
        super(type);
        this.arg = arg;
    }

    @Override
    public String toString() {
        if (arg == null)
            return "(" + type.getName() + ")";
        else
            return "(" + type.getName() + " " + arg + ")";
    }

/*
    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        if (arg == null)
            return new Instruction[]{this};
        else
            return new Instruction[]{new Instruction_1(type, arg.postprocess(ppctx))};
    }
*/

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        if (arg != null)
            arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
    }
}
