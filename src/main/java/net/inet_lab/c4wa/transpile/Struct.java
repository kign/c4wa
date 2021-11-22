package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Struct extends CType {
    final String name;
    final Map<String,Var> m;
    final int size;

    public Struct(String name, VarInput[] members) {
        this.name = name;

        m = new HashMap<>();

        int offset = 0;
        for(VarInput mem: members) {
            if (m.containsKey(mem.name))
                throw new RuntimeException("Member '" + mem.name + "' already exists");
            m.put(mem.name, new Var(mem, offset));
            offset += mem.type.size() * (mem.size == null? 1 : mem.size);
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

    static class VarInput {
        final CType type;
        final String name;
        final Integer size;

        VarInput(String name, CType type, Integer size) {
            this.name = name;
            this.type = type;
            this.size = size;
        }
    }

    static class Var {
        final CType type;
        final int offset;
        final Integer size;

        Var(VarInput v, int offset) {
            this.type = v.size == null? v.type : v.type.make_pointer_to();
            this.offset = offset;
            this.size = v.size;
        }
    }

    @Override
    public boolean is_struct() {
        return true;
    }

}
