package net.inet_lab.c4wa.wat;

public enum NumType implements WasmOutputStream.Opcode {
    I32("i32", 0x7F),
    I64("i64", 0x7E),
    F32("f32", 0x7D),
    F64("f64", 0x7C),

    // These types are limited to binary WASM format
    FUNCREF("funcref", 0x70),
    FUNC("func", 0x60),
    VOID("void", 0x40);

    final String name;
    final byte opcode;

    public String toString() { return name; }

    NumType(String name, int opcode) {
        this.name = name;
        this.opcode = (byte)opcode;
    }

    boolean is32() {
        return this == I32 || this == F32;
    }

    boolean is64() {
        return this == I64 || this == F64;
    }

    @Override
    public byte opcode() {
        return opcode;
    }
}
