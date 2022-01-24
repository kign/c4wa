package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Store extends Instruction {
    final public NumType numType;
    final public Expression address;
    final public Expression value;

    public Store(NumType numType, Expression address, Expression value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.STORE));
        this.numType = numType;
        this.address = address.comptime_eval();
        this.value = value.comptime_eval();
    }

    public Store(NumType numType, int wrap, Expression address, Expression value) {
        super(new InstructionWithNumPrefix(numType,
                (wrap == 8)? InstructionName.STORE8 : ((wrap == 16) ? InstructionName.STORE16 : InstructionName.STORE32)));
        this.numType = numType;
        this.address = address.comptime_eval();
        this.value = value.comptime_eval();
    }

    byte getAlignment() {
        if (type.getMain() == InstructionName.STORE8)
            return 0x00;
        else if (type.getMain() == InstructionName.STORE16)
            return 0x01;
        else if (numType.is32())
            return 0x02;
        else
            return 0x03;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + address + " " + value + ")";
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        Expression ppAddress = address.postprocess(ppctx);
        Expression ppValue = value.postprocess(ppctx);
        Store store = type.getMain() == InstructionName.STORE8 ? new Store(numType, 8, ppAddress, ppValue)
                      : type.getMain() == InstructionName.STORE16 ? new Store(numType, 16, ppAddress, ppValue)
                      : new Store(numType, ppAddress, ppValue);
        return new Instruction[]{store};
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        address.wasm(mCtx, fCtx, out);
        value.wasm(mCtx, fCtx, out);
        out.writeOpcode(type);
        out.writeDirect(new byte[]{((Store) this).getAlignment(), 0x00});
    }

}
