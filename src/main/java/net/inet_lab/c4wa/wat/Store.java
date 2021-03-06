package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Store extends Instruction {
    final public NumType numType;
    final public Expression address;
    final public Expression value;
    final private int alignment;

    public Store(NumType numType, Expression address, Expression value, int alignment) {
        super(new InstructionWithNumPrefix(numType, InstructionName.STORE));
        this.numType = numType;
        this.address = address.comptime_eval();
        this.value = value.comptime_eval();
        this.alignment = alignment;
    }

    public Store(NumType numType, int wrap, Expression address, Expression value, int alignment) {
        super(new InstructionWithNumPrefix(numType,
                (wrap == 8)? InstructionName.STORE8 : ((wrap == 16) ? InstructionName.STORE16 : InstructionName.STORE32)));
        this.numType = numType;
        this.address = address.comptime_eval();
        this.value = value.comptime_eval();
        this.alignment = alignment;
    }

    private int getWrap() {
        return type.getMain() == InstructionName.STORE8 ? 8 :
               type.getMain() == InstructionName.STORE16 ? 16 :
               numType.is32() ? 32 : 64;
    }

    private int getAlignment() {
        return Math.min(alignment, getWrap() / 8);
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " align=" + getAlignment() + " " + address + " " + value + ")";
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        Expression ppAddress = address.postprocess(ppctx);
        Expression ppValue = value.postprocess(ppctx);
        Store store = type.getMain() == InstructionName.STORE8 ? new Store(numType, 8, ppAddress, ppValue, alignment)
                      : type.getMain() == InstructionName.STORE16 ? new Store(numType, 16, ppAddress, ppValue, alignment)
                      : new Store(numType, ppAddress, ppValue, alignment);
        return new Instruction[]{store};
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        address.wasm(mCtx, fCtx, out);
        value.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
        byte logA = 0;
        int align = getAlignment();
        for (; align > 1; align /= 2, logA++) ;
        out.writeDirect(new byte[]{logA, 0x00});
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        int addr = address.eval(ectx).asInt();
        if (addr % getAlignment() != 0)
            throw new RuntimeException("Alignment hint violation for " + type + ", address = " + addr +
                    ", alignment = " + this.alignment);

        ectx.memoryStore(addr, value.eval(ectx), getWrap());
    }
}
