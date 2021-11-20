package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.Const;
import net.inet_lab.c4wa.wat.Global;

class VariableDecl implements Partial {
    final CType type;
    final String name;
    boolean exported;
    boolean imported;
    boolean mutable;
    Const initialValue;

    VariableDecl(CType type, String name) {
        this.type = type;
        this.name = name;
        imported = exported = false;
        mutable = true;
        initialValue = null;
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
