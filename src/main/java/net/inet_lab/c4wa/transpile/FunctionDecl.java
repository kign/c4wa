package net.inet_lab.c4wa.transpile;

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

    public String wat() {
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
}
