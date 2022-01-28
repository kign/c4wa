package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Load extends Expression {
    final Expression arg;
    final int alignment;
    public Load(NumType numType, Expression arg, int alignment) {
        super(InstructionName.LOAD, numType);
        this.arg = arg.comptime_eval();
        this.alignment = alignment;
    }

    public Load(NumType numType, int wrap, boolean signed, Expression arg, int alignment) {
        super((wrap == 8) ?   (signed? InstructionName.LOAD8_S : InstructionName.LOAD8_U) :
            ((wrap == 16) ? (signed ? InstructionName.LOAD16_S : InstructionName.LOAD16_U) :
                            (signed ? InstructionName.LOAD32_S : InstructionName.LOAD32_U)),
        numType);
        this.arg = arg.comptime_eval();
        this.alignment = alignment;
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

    private int getAlignment() {
        return Math.min(alignment, getWrap()/8);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        Expression a1 = arg.postprocess(ppctx);
        if (name == InstructionName.LOAD)
            return new Load(numType, a1, alignment);
        else
            return new Load(numType, getWrap(), getSigned(), a1, alignment);
    }

    @Override
    public int complexity() {
        return 1 + arg.complexity();
    }

    @Override
    public String toString() {
        return "(" + fullName() + " align=" + getAlignment() + " " + arg + ")";
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
        byte logA = 0;
        int align = getAlignment();
        for (; align > 1; align /= 2, logA ++);
        out.writeDirect(new byte[]{logA, 0x00});
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        int wrap = getWrap();
        boolean signed = getSigned();
        int address = arg.eval(ectx).asInt();

        if (address % getAlignment() != 0)
            throw new RuntimeException("Alignment hint violation for " + fullName() + ", address = " + address +
                    ", alignment = " + this.alignment);

        long res = ectx.memoryLoad(address, wrap, signed);

        if (numType.is_int())
            return new Const(numType, res);
        else if (numType == NumType.F32)
            return new Const(numType, Float.intBitsToFloat((int)res));
        else
            return new Const(numType, Double.longBitsToDouble(res));
    }
}
