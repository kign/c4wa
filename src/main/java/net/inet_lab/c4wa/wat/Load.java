package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Load extends Expression {
    final Expression arg;
    public Load(NumType numType, Expression arg) {
        super(InstructionName.LOAD, numType);
        this.arg = arg.comptime_eval();
    }

    public Load(NumType numType, int wrap, boolean signed, Expression arg) {
        super((wrap == 8) ?   (signed? InstructionName.LOAD8_S : InstructionName.LOAD8_U) :
            ((wrap == 16) ? (signed ? InstructionName.LOAD16_S : InstructionName.LOAD16_U) :
                            (signed ? InstructionName.LOAD32_S : InstructionName.LOAD32_U)),
        numType);
        this.arg = arg.comptime_eval();
    }

    byte getAlignment() {
        if (name == InstructionName.LOAD8_S || name == InstructionName.LOAD8_U)
            return 0x00;
        else if (name == InstructionName.LOAD16_S || name == InstructionName.LOAD16_U)
            return 0x01;
        else if (name == InstructionName.LOAD32_S || name == InstructionName.LOAD32_U)
            return 0x02;
        else if (numType.is32())
            return 0x02;
        else
            return 0x03;
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        Expression a1 = arg.postprocess(ppctx);
        return name == InstructionName.LOAD ? new Load(numType, a1)
                : name == InstructionName.LOAD8_S ? new Load(numType, 8, true, a1)
                : name == InstructionName.LOAD8_U ? new Load(numType, 8, false, a1)
                : name == InstructionName.LOAD16_S ? new Load(numType, 16, true, a1)
                : name == InstructionName.LOAD16_U ? new Load(numType, 16, false, a1)
                : name == InstructionName.LOAD32_S ? new Load(numType, 32, true, a1)
                : name == InstructionName.LOAD32_U ? new Load(numType, 32, false, a1)
                : null;
    }

    @Override
    public int complexity() {
        return 1 + arg.complexity();
    }

    @Override
    public String toString() {
        return "(" + fullName() + " " + arg + ")";
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
        out.writeDirect(new byte[]{getAlignment(), 0x00});
    }

}
