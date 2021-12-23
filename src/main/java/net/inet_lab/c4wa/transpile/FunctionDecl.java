package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FunctionDecl implements Partial {
    final String name;
    final @NotNull CType returnType;
    final CType[] params;
    final boolean vararg;
    final SType storage;

    boolean is_used;
    SyntaxError.Position where_used;

    enum SType {
        INTERNAL, // Present in this module. Could be declared "static" to please C compiler
        EXPORTED, // Present and exported; must be defined as "extern" and optionally declared as "static"
        STATIC,   // One of the above, we don't know which one yet
        IMPORTED, // imported, must be declared with no qualifiers
        EXTERNAL, // present in another source file, must be declared as "extern"
        BUILTIN   // built-in functions
    }

    FunctionDecl(String name, @NotNull CType returnType, @NotNull CType[] params, boolean vararg, SType storage) {
        this.name = name;
        this.returnType = returnType;
        this.vararg = vararg;
        this.storage = storage;
        this.params = params;

        if (params.length == 0 && vararg)
            throw new RuntimeException("Function '" + name + "': must have at least ine argument before '...'");

        is_used = false;
        where_used = null;
    }

    void markUsed(SyntaxError.Position where_used) {
        is_used = true;
        this.where_used = where_used;
    }

    public String signature () {
        StringBuilder b = new StringBuilder();

        b.append(returnType).append(" ").append(name).append('(');
        for(int i = 0; i < params.length; i ++) {
            if (i > 0)
                b.append(", ");
            b.append(params[i]);
        }
        if (vararg)
            b.append(", ...");
        b.append(')');
        return b.toString();
    }

    public boolean canBeReplacedWith(FunctionDecl o) {
        return  storage == SType.STATIC &&
                (o.storage == SType.INTERNAL || o.storage == SType.EXPORTED);
    }

    public int legalInAnotherFile(FunctionDecl o) {
        // returns 0 if not legal, -1 if old declaration stands, 1 if new declaration rules
        if (storage == SType.IMPORTED && o.storage == SType.IMPORTED)
            return -1;
        if (storage == SType.EXTERNAL && o.storage == SType.INTERNAL)
            return 1;
        if (storage == SType.INTERNAL && o.storage == SType.EXTERNAL)
            return -1;

        return 0;
    }

    public boolean sameSignature(FunctionDecl o) {
        return o.signature().equals(signature());
    }

    public Func wat() {
        // called for imported functions only
        List<Instruction> attributes = new ArrayList<>();
        attributes.add(new Special(name));

        for (CType c : params)
            attributes.add(new Param(c.asNumType()));
        if (vararg)
            attributes.add(new Param(NumType.I32));

        if (!returnType.is_void())
            attributes.add(new Result(returnType.asNumType()));

        return new Func(attributes);
    }
}
