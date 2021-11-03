package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.Instruction;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class FunctionEnv implements Partial {
    final String name;
    final CType returnType;
    final List<String> params;
    final List<String> locals;
    final boolean export;
    int mem_offset;

    final Map<String, CType> varType;
    Instruction[] instructions;

    public FunctionEnv (String name, CType returnType, boolean export) {
        this.name = name;
        this.returnType = returnType;
        this.params = new ArrayList<>();;
        this.locals = new ArrayList<>();
        this.export = export;
        this.mem_offset = 0;

        varType = new HashMap<>();
    }

    public void setMemOffset(int offset) {
        if (offset > mem_offset)
            mem_offset = offset;
    }

    public int getMemOffset() {
        return mem_offset;
    }

    public void registerVar(String name, CType type, boolean is_param) {
        if (varType.containsKey(name))
            throw new RuntimeException("Variable " + name + " already defined");
        varType.put(name, type);
        if (is_param)
            params.add(name);
        else
            locals.add(name);
    }

    public FunctionDecl makeDeclaration() {
        return new FunctionDecl(name, returnType,
                params.stream().map(varType::get).toArray(CType[]::new), false, false);
    }

    public void addInstructions(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public void generateWat(final PrintStream out) throws IOException {
        out.print("(func " + "$" + name);

        if (export)
            out.print(" (export \"" + name + "\")");

        for (String p: params)
            out.print(" (param " + "$" + p + " " + varType.get(p).asNumType() + ")");

        if (returnType != null)
            out.print(" (result " + returnType.asNumType() + ")");

        out.println();

        for (String v : locals)
            out.println("(local $" + v + " " + varType.get(v).asNumType() + ")");

        for (Instruction e: instructions)
            out.println(e);

        out.println(")");
    }

    public String toString() {
        StringBuilder b = new StringBuilder();

        if (export)
            b.append("export ");

        b.append(returnType).append(" ").append(name).append("(");
        for (int i = 0; i < params.size(); i ++) {
            if (i > 0)
                b.append(", ");
            b.append(params.get(i));
        }

        b.append(")");

        return b.toString();
    }

}
