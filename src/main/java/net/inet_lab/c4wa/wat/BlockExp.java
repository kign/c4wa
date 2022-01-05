package net.inet_lab.c4wa.wat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockExp extends Expression {
    final String ref;
    final Instruction[] body;
    final Expression returnExp;

    public BlockExp(String ref, NumType numType, Instruction[] body, Expression returnExp) {
        super(InstructionName.BLOCK, numType);
        this.ref = ref;
        this.body = body;
        this.returnExp = returnExp;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append('(').append(name.getName());
        if (ref != null)
            b.append(" $").append(ref);
        b.append(" (result ").append(numType.name).append(')');
        for (var i: body)
            b.append(' ').append(i);
        b.append(' ').append(returnExp).append(')');

        return b.toString();
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        List<Instruction> pp = new ArrayList<>();

        for (var i : body)
            pp.addAll(Arrays.asList(i.postprocess(ppctx)));
        return new BlockExp(ref, numType, pp.toArray(Instruction[]::new), returnExp.postprocess(ppctx));
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        fCtx.blockStack.addFirst(ref);
        out.writeOpcode(this);
        out.writeOpcode(numType);
        for (Instruction i : body)
            i.wasm(mCtx, fCtx, out);
        returnExp.wasm(mCtx, fCtx, out);
        out.writeOpcode(InstructionName.END);
        fCtx.blockStack.removeFirst();
    }
}
