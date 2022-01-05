package net.inet_lab.c4wa.wat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Instruction_list extends Instruction {
    final Instruction[] attributes;
    final Instruction[] elements;

    public Instruction_list(InstructionType type, Instruction[] attributes, Instruction[] elements) {
        super(type);
        this.attributes = attributes;
        this.elements = elements;
    }

    public Instruction_list(InstructionType type, String ref, Instruction[] elements) {
        super(type);
        this.attributes = new Instruction[]{new Special(ref)};
        this.elements = elements;
    }

    public Instruction_list(InstructionType type, Instruction[] elements_or_attributes, boolean pElements) {
        super(type);
        this.attributes = pElements? null: elements_or_attributes;
        this.elements = pElements? elements_or_attributes : null;
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[] {postprocessList(ppctx)};
    }

    public Instruction_list postprocessList(PostprocessContext ppctx) {
        List<Instruction> res = new ArrayList<>();

        boolean same = true;
        for (Instruction elm: elements) {
            Instruction[] pp = elm.postprocess(ppctx);
            if (pp.length != 1 || pp[0] != elm)
                same = false;
            res.addAll(Arrays.asList(pp));
        }

        if (type == InstructionName.FUNC && !res.isEmpty()) {
            int n = res.size() - 1;
            if (res.get(n) instanceof Return) {
                same = false;
                Return r = (Return) res.get(n);
                if (r.arg == null)
                    res.remove(n);
                else
                    res.set(n, new WrapExp(r.arg));
            }
        }

        if (same)
            return this;
        else if (res.size() == 0)
            return null;
        else if (type == InstructionName.MODULE)
            return new Module(res);
        else if (type == InstructionName.FUNC)
            return new Func(Arrays.asList(attributes), res);
        else
            return new Instruction_list(type, attributes, res.toArray(Instruction[]::new));
    }

    @Override
    public String toStringPretty(int indent) {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName());
        if (attributes != null) {
            for (Instruction attribute : attributes)
                b.append(' ').append(attribute);
        }
        if (elements == null || elements.length == 0)
            b.append(')');
        else {
            b.append('\n');
            for (int i = 0; i < elements.length; i++) {
                b.append(" ".repeat(indent));
                b.append(elements[i].toStringPretty(indent + 2));
                if (i < elements.length - 1)
                    b.append('\n');
                else
                    b.append(')');
            }
        }
        return b.toString();
    }

    @Override
    public String toString() {
        return toStringPretty(0);
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        String blockName = null;
        NumType returnType = null;
        if (attributes != null) {
            for (Instruction a: attributes) {
                if (a instanceof Special && ((Special)a).ref != null && blockName == null)
                    blockName = ((Special) a).ref;
                else if (a instanceof Result && returnType == null) {
                    returnType = ((Result) a).numType;
                    throw new RuntimeException("That's what BlockExp is for");
                }
                else
                    throw new RuntimeException(type + " with unknown attribute " + a + "; class = " + getClass().getSimpleName());
            }
        }
        if (returnType == null)
            returnType = NumType.VOID;
        if (blockName == null)
            blockName = "<noname>";
        fCtx.blockStack.addFirst(blockName);
        if (type != InstructionName.THEN)
            out.writeOpcode(type);
        if (type != InstructionName.THEN && type != InstructionName.ELSE)
            out.writeOpcode(returnType);
        for (Instruction i: elements)
            i.wasm(mCtx, fCtx, out);
        if (type != InstructionName.THEN && type != InstructionName.ELSE)
            out.writeOpcode(InstructionName.END);
        fCtx.blockStack.removeFirst();
    }
}
