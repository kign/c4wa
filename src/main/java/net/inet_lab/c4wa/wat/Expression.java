package net.inet_lab.c4wa.wat;

import java.io.IOException;

abstract public class Expression implements WasmOutputStream.Opcode {
    final InstructionName name;
    final NumType numType;

    Expression(InstructionName name, NumType numType) {
        this.name = name;
        this.numType = numType;
    }

    String fullName() {
        if (numType == null)
            return name.getName();
        else
            return numType.name + "." + name.getName();
    }

    abstract public String toString();

    public int complexity() {
        return 1000;
    }

    public Expression comptime_eval() {
        return this;
    }

    public Expression postprocess(PostprocessContext ppctx) {
        return this;
    }

    public Expression Not(NumType numType) {
        // NOTE: The type of the result is always I32. `numType` is the type of "this"
        if (numType == NumType.I32 || numType == NumType.I64)
            return new Eqz(numType, this);
        else
            throw new RuntimeException("Cannot take logical negative of '" + fullName() + "'");
    }

    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        throw new RuntimeException("Not yet implemented for " + numType + "."  + name);
    }

    @Override
    public byte opcode() {
        if (numType == null ||
                name == InstructionName.GET_LOCAL ||
                name == InstructionName.TEE_LOCAL ||
                name == InstructionName.GET_GLOBAL ||
                name == InstructionName.BR_IF ||
                name == InstructionName.BLOCK ||
                name == InstructionName.CALL)
            return name.opcode();
        else
            return name.opcode(numType);
    }
}
