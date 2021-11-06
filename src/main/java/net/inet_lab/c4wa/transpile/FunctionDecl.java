package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;

import java.util.ArrayList;
import java.util.List;

public class FunctionDecl implements Partial {
    final String name;
    final CType returnType;
    final CType[] params;
    final boolean anytype;
    final boolean imported;

    FunctionDecl(String name, CType returnType, CType[] params, boolean anytype, boolean imported) {
        this.name = name;
        this.returnType = returnType;
        this.anytype = anytype;
        this.params = anytype? null : params;
        this.imported = imported;
    }

    public Func wat() {
        List<Instruction> attributes = new ArrayList<>();
        attributes.add(new Special(name));

        if (anytype) {
            attributes.add(new Param(NumType.I32));
            attributes.add(new Param(NumType.I32));
        }
        else if (params != null) {
            for (CType c : params)
                attributes.add(new Param(c.asNumType()));
        }

        if (returnType != null)
            attributes.add(new Result(returnType.asNumType()));

        return new Func(attributes);
    }

    /*
    public String wat_TBR() {
        StringBuilder b = new StringBuilder();

        b.append("(func $").append(name);
        if (anytype) {
            b.append(" (param i32) (param i32)");
        }
        else if (params != null) {
            for (CType c : params)
                b.append(" (param ").append(c.asNumType()).append(")");
        }
        if (returnType != null)
            b.append("(result ").append(returnType.asNumType()).append(")");
        b.append(")");

        return b.toString();
    }
     */
}
