package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.Expression;
import net.inet_lab.c4wa.wat.Global;

class VariableDecl implements Partial {
    final CType type;
    final String name;
    boolean exported;
    boolean imported;
    boolean mutable;
    boolean inStack;
    boolean isArray;
    Expression initialValue;
    boolean is_used;

    final SyntaxError.Position where_defined;

    VariableDecl(CType type, String name, boolean is_mutable, SyntaxError.Position where_defined) {
        this.type = type;
        this.name = name;
        this.where_defined = where_defined;

        imported = exported = inStack = isArray = false;
        mutable = is_mutable;
        initialValue = null;
        is_used = false;
    }

    public void markUsed() {
        is_used = true;
    }

    public Global wat (String gobalImportName) {
        if (exported)
            return new Global(name, name, type.asNumType(), mutable, initialValue);
        else if (imported)
            return new Global(name, gobalImportName, name, type.asNumType(), mutable);
        else
            return new Global(name, type.asNumType(), mutable, initialValue);
    }

    public Global wat () {
        if (imported)
            throw new RuntimeException("Incorrect wat() call with no arguments for imported variable");
        if (exported)
            return new Global(name, name, type.asNumType(), mutable, initialValue);
        else
            return new Global(name, type.asNumType(), mutable, initialValue);
    }
}
