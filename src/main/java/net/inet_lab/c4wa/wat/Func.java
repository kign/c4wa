package net.inet_lab.c4wa.wat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Func extends Instruction_list {
    final byte OP_WASM_FUNC = 0x60;

    public Func(List<Instruction> attributes, List<Instruction> elements) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), elements.toArray(Instruction[]::new));
    }

    public Func(List<Instruction> attributes) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), false);
    }

    public WasmOutputStream wasmSignature() throws IOException {
        WasmOutputStream bout = new WasmOutputStream();
        List<Param> params = new ArrayList<>();
        List<Result> results = new ArrayList<>();
        for (Instruction i: attributes)
            if (i.type == InstructionName.PARAM)
                params.add((Param) i);
            else if (i.type == InstructionName.RESULT)
                results.add((Result) i);
        bout.writeInt(OP_WASM_FUNC);
        bout.writeInt(params.size());
        for (Param p: params)
            bout.writeOpcode(p.numType);
        bout.writeInt(results.size());
        for (Result r: results)
            bout.writeOpcode(r.numType);

        return bout;
    }

    public String getName () {
        for (Instruction i : attributes)
            if (i.type == InstructionName.SPECIAL)
                return ((Special)i).ref;
        return null;
    }

    public Export getExport() {
        for (Instruction i : attributes)
            if (i.type == InstructionName.EXPORT)
                return (Export) i;
        return null;
    }

    WasmOutputStream makeWasm(Module.WasmContext mCtx) throws IOException {
        int[] counts_1 = {0, 0, 0, 0};
        int[] counts_2 = {0, 0, 0, 0};
        NumType[] types = {null, null, null, null};
        int n_types = 0;

        for (Instruction i : elements) {
            if (i.type == InstructionName.LOCAL) {
                Local iLocal = (Local) i;
                int idx;
                for (idx = 0; idx < n_types && types[idx] != iLocal.numType; idx ++);
                if (idx == n_types) {
                    types[n_types] = iLocal.numType;
                    n_types ++;
                }
                counts_1[idx] ++;
            } else
                break;
        }

        WasmOutputStream out = new WasmOutputStream();

        out.writeInt(n_types);
        for (int idx = 0; idx < n_types; idx ++) {
            out.writeInt(counts_1[idx]);
            out.writeOpcode(types[idx]);
        }

        WasmContext fCtx = new WasmContext();
        int n_params = 0;
        for (Instruction i : attributes)
            if (i.type == InstructionName.PARAM) {
                Param iParam = (Param) i;
                fCtx.locals.put(iParam.ref, n_params);
                n_params ++;
            }

        for (Instruction i : elements) {
            if (i.type == InstructionName.LOCAL) {
                Local iLocal = (Local) i;

                int idx, cnt = 0;
                for (idx = 0; idx < n_types && types[idx] != iLocal.numType; idx++)
                    cnt += counts_1[idx];

                fCtx.locals.put(iLocal.ref, n_params + cnt + counts_2[idx]);
                counts_2[idx] ++;

            }
            else
                i.wasm(mCtx, fCtx, out);
        }

        out.writeOpcode(InstructionName.END);

        return out;
    }

    static class WasmContext {
        final Map<String,Integer> locals;

        WasmContext() {
            this.locals = new HashMap<>();
        }
    }
}
