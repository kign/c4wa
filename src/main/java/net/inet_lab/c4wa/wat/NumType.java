package net.inet_lab.c4wa.wat;

public enum NumType implements WasmOutputStream.Opcode {
    I32("i32", 0x7F),
    I64("i64", 0x7E),
    F32("f32", 0x7D),
    F64("f64", 0x7C);

    final String name;
    final byte opcode;

    public String toString() { return name; }

    NumType(String name, int opcode) {
        this.name = name;
        this.opcode = (byte)opcode;
    }

    @Override
    public byte opcode() {
        return opcode;
    }
}
