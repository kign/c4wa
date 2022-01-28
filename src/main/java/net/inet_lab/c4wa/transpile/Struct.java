package net.inet_lab.c4wa.transpile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Struct extends StructDecl {
    final Map<String,Var> m;
    final int size;

    public Struct(String name, VarInput[] members, int alignment) {
        super(name);
        m = new HashMap<>();

        int offset = 0;
        for(VarInput mem: members) {
            if (m.containsKey(mem.name))
                throw new RuntimeException("Member '" + mem.name + "' already exists");
            int align = Math.min(mem.type.size(), alignment);
            if (align > 1 && offset % align > 0)
                offset += align - offset % align;
            m.put(mem.name, new Var(mem, offset));
            offset += mem.type.size() * (mem.size == null? 1 : mem.size);
        }

        size = offset;
    }

    public boolean sameSchema(Struct o) {
        if (size != o.size)
            return false;
        for (String key: m.keySet()) {
            Var v1 = m.get(key);
            Var v2 = o.m.get(key);
            if (v2 == null || !v1.same(v2))
                return false;
        }

        return true;
    }

    @Override
    public int size() {
        return size;
    }


    @Override
    public boolean is_undefined_struct() {
        return false;
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

        boolean same(Var o) {
            return type.same(o.type) && offset == o.offset && Objects.equals(size, o.size);
        }
    }
}
