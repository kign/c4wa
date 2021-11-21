package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.NumType;

import java.nio.file.ClosedDirectoryStreamException;

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

    public static class Pointer extends CType {
        final CType base;

        public Pointer(CType base) {
            this.base = base;
        }

        @Override
        public boolean isValidRHS(CType rhs) {
            return rhs instanceof Pointer && base.isValidRHS(((Pointer)rhs).base);
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
            if (primitiveType == PrimitiveType.CHAR)
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
    public boolean same(CType rhs) { return isValidRHS(rhs) && rhs.isValidRHS(this); }
    public CType deref() { return null; }
    public boolean is_ptr() { return deref() != null; }
    public boolean is_primitive() { return false; }
    public boolean is_signed() { return true; }
    public boolean is_struct() { return false; }

    public CType make_signed(boolean signed) {
        throw new RuntimeException("Not applicable to " + this);
    }

    public CType make_pointer_to() { return new Pointer(this); }
    abstract public NumType asNumType();
    abstract public int size();

    abstract public String toString();
}
