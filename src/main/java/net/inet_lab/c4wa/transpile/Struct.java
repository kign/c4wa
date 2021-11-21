package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

import java.util.HashMap;
import java.util.Map;

public class Struct extends CType {
    final String name;
    final Map<String,Var> m;
    final int size;

    public Struct(String name, VariableDecl[] members) {
        this.name = name;

        m = new HashMap<>();

        int offset = 0;
        for(VariableDecl mem: members) {
            if (m.containsKey(mem.name))
                throw new RuntimeException("Member '" + mem.name + "' already exists");
            m.put(mem.name, new Var(mem.type, offset));
            offset += mem.type.size();
        }

        size = offset;
    }

    @Override
    public NumType asNumType() {
        throw new RuntimeException("struct can't be converted to Web Assembly numeric type");
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "struct " + name;
    }

    @Override
    public boolean isValidRHS(CType rhs) {
        return rhs instanceof Struct && name.equals(((Struct) rhs).name);
    }

    static class Var {
        final CType type;
        final int offset;

        Var(CType type, int offset) {
            this.type = type;
            this.offset = offset;
        }
    }

    public boolean is_struct() {
        return true;
    }
}
