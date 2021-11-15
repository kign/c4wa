package net.inet_lab.c4wa.wat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Special extends Instruction {
    final byte[] bytes;
    final String ref;
    final NumType numType;
    final Integer fixedInt;

    Special(byte[] bytes) {
        super(InstructionName.SPECIAL);
        this.bytes = bytes;
        ref = null;
        numType = null;
        fixedInt = null;
    }

    Special(byte[] bytes, int len) {
        super(InstructionName.SPECIAL);
        this.bytes = Arrays.copyOf(bytes, len);
        ref = null;
        numType = null;
        fixedInt = null;
    }

    public Special(String ref) {
        super(InstructionName.SPECIAL);
        bytes = null;
        this.ref = ref;
        numType = null;
        fixedInt = null;
    }

    Special(NumType numType) {
        super(InstructionName.SPECIAL);
        bytes = null;
        ref = null;
        this.numType = numType;
        fixedInt = null;
    }

    Special(int fixedInt) {
        super(InstructionName.SPECIAL);
        bytes = null;
        ref = null;
        numType = null;
        this.fixedInt = fixedInt;
    }

    @Override
    public String toString() {
        if (bytes != null) {
            StringBuilder res = new StringBuilder();
            res.append('"');
            for (byte b : bytes) {
                if (0x20 <= b && b <= 0x7e && b != '\\')
                    res.append((char) b);
                else
                    res.append(String.format("\\%02X", b));
            }
            res.append('"');
            return res.toString();
        }
        else if (ref != null)
            return "$" + ref;
        else if (numType != null)
            return numType.toString();
        else
            return fixedInt.toString();
    }

    @Override
    public int complexity() {
        return 0;
    }
}
