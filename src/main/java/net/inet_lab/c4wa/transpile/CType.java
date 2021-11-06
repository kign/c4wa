package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

abstract public class CType implements Partial {
    static public CType CHAR = getPrimitive(PrimitiveType.CHAR);
    static public CType SHORT = getPrimitive(PrimitiveType.SHORT);
    static public CType INT = getPrimitive(PrimitiveType.INT);
    static public CType LONG = getPrimitive(PrimitiveType.LONG);
    static public CType UNSIGNED_CHAR = getPrimitiveUnsigned(PrimitiveType.CHAR);
    static public CType UNSIGNED_SHORT = getPrimitiveUnsigned(PrimitiveType.SHORT);
    static public CType UNSIGNED_INT = getPrimitiveUnsigned(PrimitiveType.INT);
    static public CType UNSIGNED_LONG = getPrimitiveUnsigned(PrimitiveType.LONG);
    static public CType FLOAT = getPrimitive(PrimitiveType.FLOAT);
    static public CType DOUBLE = getPrimitive(PrimitiveType.DOUBLE);

    public enum PrimitiveType {
        CHAR, SHORT, INT, LONG, FLOAT, DOUBLE;
    }

    private static CType getPrimitive(PrimitiveType primitiveType) {
        return new Primitive(primitiveType, true);
    }

    private static CType getPrimitiveUnsigned(PrimitiveType primitiveType) {
        return new Primitive(primitiveType, false);
    }

    public static class Primitive extends CType {
        final PrimitiveType primitiveType;
        final boolean signed;

        @Override
        public boolean is_signed() {
            return signed;
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
    public boolean isValidRHS(CType rhs) { return false; }
    public boolean is_ptr() { return false; }
    public boolean is_signed() { return true; }
    abstract public NumType asNumType();

    abstract public String toString();
}
