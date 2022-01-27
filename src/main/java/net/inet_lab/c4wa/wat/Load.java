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

    private int getWrap () {
        switch (name) {
            case LOAD8_S:
            case LOAD8_U:
                return 8;
            case LOAD16_S:
            case LOAD16_U:
                return 16;
            case LOAD32_S:
            case LOAD32_U:
                return 32;
            case LOAD:
                return numType.is32() ? 32 : 64;
            default:
                throw new RuntimeException("Invalid name = " + name);
        }
    }

    private boolean getSigned() {
        switch (name) {
            case LOAD8_U:
            case LOAD16_U:
            case LOAD32_U:
                return false;
            default:
                return true;
        }
    }

    private byte getAlignment() {
        int wrap = getWrap();
        byte align = 0;
        while (wrap > 8) {
            wrap /= 2;
            align ++;
        }
        return align;
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        Expression a1 = arg.postprocess(ppctx);
        if (name == InstructionName.LOAD)
            return new Load(numType, a1);
        else
            return new Load(numType, getWrap(), getSigned(), a1);
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

    @Override
    public Const eval(ExecutionCtx ectx) {
        int wrap = getWrap();
        boolean signed = getSigned();
        int address = arg.eval(ectx).asInt();

        long res = ectx.memoryLoad(address, wrap, signed);

        if (numType.is_int())
            return new Const(numType, res);
        else if (numType == NumType.F32)
            return new Const(numType, Float.intBitsToFloat((int)res));
        else
            return new Const(numType, Double.longBitsToDouble(res));
    }
}
