package net.inet_lab.c4wa.wat;

public enum NumType {
    I32("i32"),
    I64("i64"),
    F32("f32"),
    F64("f64");

    final String name;

    public String toString() { return name; }

    NumType(String name) {
        this.name = name;
    }
}
