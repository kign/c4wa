package net.inet_lab.c4wa.wat;

import java.nio.charset.StandardCharsets;

public class Export extends Instruction {
    final byte[] exportName;
    public Export(String name) {
        super(InstructionName.EXPORT);
        exportName = name.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName()).append(' ');
        Instruction.watWriteBytes(b, exportName);
        b.append(')');
        return b.toString();
    }
}
