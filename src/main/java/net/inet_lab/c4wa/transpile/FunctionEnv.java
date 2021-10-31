package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.Instruction;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class FunctionEnv implements Partial {
    final String name;
    final CType returnType;
    final String[] params;
    final boolean export;

    final Map<String, CType> locals;
    Instruction[] instructions;

    public FunctionEnv (String name, CType returnType, boolean export, String[] params) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.export = export;

        locals = new HashMap<>();
    }

    public void registerVar(String name, CType type) {
        if (locals.containsKey(name))
            throw new RuntimeException("Variable " + name + " already defined");
        locals.put(name, type);
    }

    public void addInstructions(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public void generateWat(final PrintStream out) throws IOException {
        out.print("(func " + "$" + name);

        if (export)
            out.print(" (export \"" + name + "\")");

        for (String p: params)
            out.print(" (param " + "$" + p + " " + locals.get(p).asNumType() + ")");

        if (returnType != null)
            out.print(" (result " + returnType.asNumType() + ")");

        out.println();

        for (Instruction e: instructions)
            out.println(e);

        out.println(")");
    }

    public String toString() {
        StringBuilder b = new StringBuilder();

        if (export)
            b.append("export ");

        b.append(returnType).append(" ").append(name).append("(");
        for (int i = 0; i < params.length; i ++) {
            if (i > 0)
                b.append(", ");
            b.append(params[i]);
        }

        b.append(")");

        return b.toString();
    }

}
