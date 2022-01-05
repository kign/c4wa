package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class Expression_2ref extends Expression {
    final String ref;
    final Expression arg1;
    final Expression arg2;

    public Expression_2ref(InstructionName name, NumType numType, String ref, Expression arg1, Expression arg2) {
        super(name, numType);
        this.ref = ref;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return "(" + name.getName() + " $" + ref + " " + arg1 + " " + arg2 + ")";
    }

    @Override
    public int complexity() {
        return 1 + Math.max(arg1.complexity(), arg2.complexity());
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Expression_2ref(name, numType, ref, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg1.wasm(mCtx, fCtx, out);
        arg2.wasm(mCtx, fCtx, out);
        out.writeOpcode(this);
        if (name == InstructionName.BR_IF) {
            int idx = 0;
            for (String blockId : fCtx.blockStack) {
                if (blockId.equals(ref))
                    break;
                idx++;
            }
            if (idx == fCtx.blockStack.size())
                throw new RuntimeException("Cannot find block label " + ref);
            out.writeUnsignedInt(idx);
        }
        else
            throw new RuntimeException("Not yet implemented for " + this);
    }
}
