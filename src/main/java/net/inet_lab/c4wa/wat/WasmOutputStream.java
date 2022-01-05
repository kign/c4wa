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

    void writeUnsignedInt(int value) throws IOException {
        // https://android.googlesource.com/platform/libcore/+/522b917/dex/src/main/java/com/android/dex/Leb128.java
        int remaining = value >>> 7;
        while (remaining != 0) {
            out.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }
        out.write((byte) (value & 0x7f));
    }

    void writeSignedInt(int value) throws IOException {
        int remaining = value >> 7;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;
        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));
            out.write((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }
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
        writeUnsignedInt(str.length);
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
        if (items.isEmpty())
            return;

        writeOpcode(section);
        WasmOutputStream sub = new WasmOutputStream();
        sub.writeUnsignedInt(items.size());
        for (WasmOutputStream item : items)
            sub.writeSubStream(item);
        writeUnsignedInt(sub.size());
        writeSubStream(sub);
    }

    interface Opcode {
        byte opcode();
    }
}
