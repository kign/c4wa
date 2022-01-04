package net.inet_lab.c4wa.wat;

import java.util.Arrays;

public class Data extends Instruction {
    final Expression offset;
    final byte[] data;

    public Data(int offset, byte[] data, int data_len) {
        super(InstructionName.DATA);
        this.offset = new Const(offset);
        this.data = Arrays.copyOf(data, data_len);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName()).append(' ').append(offset).append(' ');
        Instruction.watWriteBytes(b, data);
        b.append(')');
        return b.toString();
    }
}
