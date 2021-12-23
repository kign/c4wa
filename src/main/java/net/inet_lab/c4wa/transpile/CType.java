package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

abstract public class CType implements Partial {
    static public final CType CHAR = getPrimitive(PrimitiveType.CHAR);
    static public final CType SHORT = getPrimitive(PrimitiveType.SHORT);
    static public final CType INT = getPrimitive(PrimitiveType.INT);
    static public final CType LONG = getPrimitive(PrimitiveType.LONG);
    static public final CType UNSIGNED_CHAR = getPrimitiveUnsigned(PrimitiveType.CHAR);
    static public final CType UNSIGNED_SHORT = getPrimitiveUnsigned(PrimitiveType.SHORT);
    static public final CType UNSIGNED_INT = getPrimitiveUnsigned(PrimitiveType.INT);
    static public final CType UNSIGNED_LONG = getPrimitiveUnsigned(PrimitiveType.LONG);
    static public final CType FLOAT = getPrimitive(PrimitiveType.FLOAT);
    static public final CType DOUBLE = getPrimitive(PrimitiveType.DOUBLE);
    static public final CType VOID = getPrimitive(PrimitiveType.VOID);

    public enum PrimitiveType {
        CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, VOID
    }

    private static CType getPrimitive(PrimitiveType primitiveType) {
        return new Primitive(primitiveType, true);
    }

    private static CType getPrimitiveUnsigned(PrimitiveType primitiveType) {
        return new Primitive(primitiveType, false);
    }

    public static class Pointer extends CType {
        final CType base;

        public Pointer(CType base) {
            this.base = base;
        }

        @Override
        public boolean isValidRHS(CType rhs) {
            return rhs instanceof Pointer && (base.same(rhs.deref()) || base.is_void() || rhs.deref().is_void());
        }

        @Override
        public boolean same(CType rhs) {
            return rhs instanceof Pointer && base.same(((Pointer) rhs).base);
        }

        @Override
        public CType deref() {
            return base;
        }

        @Override
        public NumType asNumType() {
            return NumType.I32;
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public String toString() {
            return base + "*";
        }
    }

    public static class Primitive extends CType {
        final PrimitiveType primitiveType;
        final boolean signed;

        @Override
        public CType make_signed(boolean signed) {
            if (signed == this.signed)
                return this;
            return new Primitive(primitiveType, signed);
        }

        @Override
        public boolean is_signed() {
            return signed;
        }

        @Override
        public boolean is_primitive() {
            return true;
        }

        @Override
        public boolean is_i32() {
            return primitiveType == PrimitiveType.CHAR || primitiveType == PrimitiveType.SHORT || primitiveType == PrimitiveType.INT;
        }

        @Override
        public boolean is_i64() {
            return primitiveType == PrimitiveType.LONG;
        }

        @Override
        public boolean is_f32() {
            return primitiveType == PrimitiveType.FLOAT;
        }

        @Override
        public boolean is_f64() {
            return primitiveType == PrimitiveType.DOUBLE;
        }

        @Override
        public boolean isValidRHS(CType rhs) {
            if (!(rhs instanceof Primitive))
                return false;
            PrimitiveType rhsType = ((Primitive) rhs).primitiveType;
            if (rhsType == primitiveType && ((Primitive) rhs).signed == signed)
                return true;
            if (rhsType == PrimitiveType.CHAR && (primitiveType == PrimitiveType.SHORT  ||
                                                  primitiveType == PrimitiveType.INT    ||
                                                  primitiveType == PrimitiveType.LONG   ||
                                                  primitiveType == PrimitiveType.FLOAT  ||
                                                  primitiveType == PrimitiveType.DOUBLE))
                return true;
            if (rhsType == PrimitiveType.SHORT && (primitiveType == PrimitiveType.INT   ||
                                                   primitiveType == PrimitiveType.LONG  ||
                                                   primitiveType == PrimitiveType.FLOAT ||
                                                   primitiveType == PrimitiveType.DOUBLE))
                return true;
            if (rhsType == PrimitiveType.INT && (primitiveType == PrimitiveType.LONG    ||
                                                 primitiveType == PrimitiveType.FLOAT   ||
                                                 primitiveType == PrimitiveType.DOUBLE))
                return true;
            if (rhsType == PrimitiveType.LONG && (primitiveType == PrimitiveType.FLOAT   ||
                                                  primitiveType == PrimitiveType.DOUBLE))
                return true;
            if (rhsType == PrimitiveType.FLOAT && primitiveType == PrimitiveType.DOUBLE)
                return true;

            return false;
        }

        @Override
        public boolean same(CType rhs) {
            return rhs instanceof Primitive &&
                    ((Primitive)rhs).primitiveType == primitiveType &&
                    ((Primitive) rhs).signed == signed;
        }

        @Override
        public NumType asNumType() {
            if (is_i32())
                return NumType.I32;
            else if (is_i64())
                return NumType.I64;
            else if (is_f32())
                return NumType.F32;
            else if (is_f64())
                return NumType.F64;
            else
                throw new RuntimeException("Invalid type " + this);
        }

        @Override
        public int size() {
            if (primitiveType == PrimitiveType.CHAR || primitiveType == PrimitiveType.VOID)
                return 1;
            if (primitiveType == PrimitiveType.SHORT)
                return 2;
            if (primitiveType == PrimitiveType.INT)
                return 4;
            if (primitiveType == PrimitiveType.LONG)
                return 8;
            if (primitiveType == PrimitiveType.FLOAT)
                return 4;
            if (primitiveType == PrimitiveType.DOUBLE)
                return 8;

            throw new RuntimeException("size(" + primitiveType + ") is not defined");
        }

        @Override
        public boolean is_void() {
            return primitiveType == PrimitiveType.VOID;
        }

        public Primitive(PrimitiveType primitiveType, boolean signed) {
            this.primitiveType = primitiveType;
            this.signed = signed;
        }

        @Override
        public String toString() {
            return (signed?"":"UNSIGNED ") + primitiveType.toString();
        }
    }
    public boolean is_i32() { return false; }
    public boolean is_i64() { return false; }
    public boolean is_int() { return is_i32() || is_i64(); }
    public boolean is_32()  { return is_i32() || is_f32(); }
    public boolean is_64()  { return is_i64() || is_f64(); }
    public boolean is_f32() { return false; }
    public boolean is_f64() { return false; }
    public boolean is_float() { return is_f32() || is_f64(); }
    public boolean isValidRHS(CType rhs) { return false; }
    public boolean same(CType rhs) { return false; }
    public CType deref() { return null; }
    public boolean is_ptr() { return deref() != null; }
    public boolean is_primitive() { return false; }
    public boolean is_signed() { return true; }
    public boolean is_struct() { return false; }
    public boolean is_undefined_struct() { return false; }
    public boolean is_struct(String name) { return false; }
    public boolean is_void() { return false; }

    public CType make_signed(boolean signed) {
        throw new RuntimeException("Not applicable to " + this);
    }

    public CType make_pointer_to() { return new Pointer(this); }
    abstract public NumType asNumType();
    abstract public int size();

    abstract public String toString();
}
