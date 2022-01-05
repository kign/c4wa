package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Instruction_2 extends Instruction {
    final public Expression arg1;
    final public Expression arg2;

    public Instruction_2(InstructionType type, Expression arg1, Expression arg2) {
        super(type);
        this.arg1 = arg1.comptime_eval();
        this.arg2 = arg2.comptime_eval();
    }

    @Override
    public String toStringPretty(int indent) {
        return toString();
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + ")";
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new Instruction_2(type, arg1.postprocess(ppctx), arg2.postprocess(ppctx))};
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg1.wasm(mCtx, fCtx, out);
        arg2.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
        if (type.getMain() == InstructionName.STORE ||
        type.getMain() == InstructionName.STORE8 ||
        type.getMain() == InstructionName.STORE16 ||
        type.getMain() == InstructionName.STORE32 )
            out.writeDirect(new byte[]{type.getNumType().is64()? (byte)0x03 : (byte)0x02, 0x00}); // i64.store 3 0 : I have no idea
    }
}
