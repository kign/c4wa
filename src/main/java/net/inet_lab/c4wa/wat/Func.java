package net.inet_lab.c4wa.wat;

import java.io.IOException;
import java.util.*;

public class Func extends Instruction_list {
    public Func(List<Instruction> attributes, List<Instruction> elements) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), elements.toArray(Instruction[]::new));
    }

    public Func(List<Instruction> attributes) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), false);
    }

    public WasmType wasmSignature() {
        WasmType wasmType = new WasmType();
        for (Instruction i: attributes)
            if (i.type == InstructionName.PARAM)
                wasmType.params.add((Param) i);
            else if (i.type == InstructionName.RESULT)
                wasmType.results.add((Result) i);

        return wasmType;
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

        out.writeUnsignedInt(n_types);
        for (int idx = 0; idx < n_types; idx ++) {
            out.writeUnsignedInt(counts_1[idx]);
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

    static class WasmType {
        final List<Param> params;
        final List<Result> results;

        private WasmType() {
            params = new ArrayList<>();
            results = new ArrayList<>();
        }

        WasmOutputStream wasm()  {
            // used in stream filter so no exceptions
            WasmOutputStream bout = new WasmOutputStream();
            try {
                bout.writeOpcode(NumType.FUNC);
                bout.writeUnsignedInt(params.size());
                for (Param p : params)
                    bout.writeOpcode(p.numType);
                bout.writeUnsignedInt(results.size());
                for (Result r : results)
                    bout.writeOpcode(r.numType);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            return bout;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();

            for(Param p: params)
                b.append(p.numType).append(' ');

            if (results.isEmpty())
                b.append("-> void");
            else
                b.append("-> ").append(results.get(0).numType);

            return b.toString();
        }

        boolean same(WasmType o) {
            if (params.size() != o.params.size())
                return false;
            if (results.size() != o.results.size())
                return false;
            if (!results.isEmpty() && results.get(0).numType != o.results.get(0).numType)
                return false;
            for (int i = 0; i < params.size(); i ++)
                if (params.get(i).numType != o.params.get(i).numType)
                    return false;

            return true;
        }
    }

    static class WasmContext {
        final Map<String,Integer> locals;
        final Deque<String> blockStack;

        WasmContext() {
            this.locals = new HashMap<>();
            this.blockStack = new ArrayDeque<>();
        }
    }
}
