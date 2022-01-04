package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Param extends Instruction {
    final @Nullable String ref;
    final NumType numType;

    public Param(@NotNull String ref, NumType numType) {
        super(InstructionName.PARAM);
        this.ref = ref;
        this.numType = numType;
    }

    public Param(NumType numType) {
        super(InstructionName.PARAM);
        this.ref = null;
        this.numType = numType;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName());
        if (ref != null)
            b.append(" $").append(ref);

        b.append(' ').append(numType);
        b.append(')');

        return b.toString();
    }
}
