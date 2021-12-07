package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

public class StructDecl extends CType {
    final String name;

    StructDecl(String name) {
        this.name = name;
    }

    @Override
    public NumType asNumType() {
        throw new RuntimeException("struct " + name + " can't be converted to Web Assembly numeric type");
    }

    @Override
    public int size() {
        throw new RuntimeException("struct " + name + " is not defined so no size");
    }

    @Override
    public String toString() {
        return "struct " + name;
    }

    @Override
    public boolean isValidRHS(CType rhs) {
        return rhs.is_struct(name);
    }

    @Override
    public boolean is_struct() {
        return true;
    }

    @Override
    public boolean is_struct(String name) {
        return this.name.equals(name);
    }

    @Override
    public boolean is_undefined_struct() {
        return true;
    }
}
