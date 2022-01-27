package net.inet_lab.c4wa.wat;

import java.io.IOException;

public abstract class Instruction {
    final public InstructionType type;

    public Instruction(InstructionType type) {
        this.type = type;
    }

    public String toStringPretty(int indent) {
        return toString();
    }

    abstract public String toString();

    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{this};
    }

    static void watWriteBytes(StringBuilder res, byte[] bytes) {
        res.append('"');
        for (byte b : bytes) {
            if (0x20 <= b && b <= 0x7e && b != '\\' && b != '"')
                res.append((char) b);
            else
                res.append(String.format("\\%02X", b));
        }
        res.append('"');
    }

    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        throw new RuntimeException("Not yet implemented for " + type);
    }

    public void execute(ExecutionCtx ectx) {
        throw new RuntimeException("Not yet implemented for " + type);
    }
}
