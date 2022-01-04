package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class WasmOutputStream {
    final OutputStream out;
    public WasmOutputStream(@NotNull String name) throws FileNotFoundException {
        out = new FileOutputStream(name);
    }

    WasmOutputStream() {
        out = new ByteArrayOutputStream();
    }

    private ByteArrayOutputStream asByteArrayOutputStream () {
        return (ByteArrayOutputStream)this.out;
    }

    void writeInt(int value) throws IOException {
        int remaining = value >>> 7;
        while (remaining != 0) {
            out.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }
        out.write((byte) (value & 0x7f));
    }

    void writeLong(long value) throws IOException {
        long remaining = value >>> 7;
        while (remaining != 0) {
            out.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }
        out.write((byte) (value & 0x7f));
    }

    void writeFloat(float value) throws IOException {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(Float.floatToRawIntBits(value));
        writeDirect(bb.array());
    }

    void writeDouble(double value) throws IOException {
        final ByteBuffer bb = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(Double.doubleToRawLongBits(value));
        writeDirect(bb.array());
    }

    int size () {
        return asByteArrayOutputStream().size();
    }

    void writeSubStream(WasmOutputStream sub) throws IOException {
        sub.asByteArrayOutputStream().writeTo(this.out);
    }

    void writeString(byte[] str) throws IOException {
        writeInt(str.length);
        writeDirect(str);
    }

    void writeDirect(byte[] bytes) throws IOException {
        out.write(bytes);
    }

    void writeDirect(byte b) throws IOException {
        out.write(b);
    }

    void writeOpcode(Opcode op) throws IOException {
        out.write(op.opcode());
    }

    void writeSection(Opcode section, List<WasmOutputStream> items) throws IOException {
        writeOpcode(section);
        WasmOutputStream sub = new WasmOutputStream();
        sub.writeInt(items.size());
        for (WasmOutputStream item : items)
            sub.writeSubStream(item);
        writeInt(sub.size());
        writeSubStream(sub);
    }

    interface Opcode {
        byte opcode();
    }
}
