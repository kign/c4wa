package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class Import extends Instruction {
    final byte[] importModule;
    final byte[] importName;
    final @Nullable Instruction decl;

    public Import(String importModule, String importName) {
        super(InstructionName.IMPORT);
        this.importModule = importModule.getBytes(StandardCharsets.UTF_8);
        this.importName = importName.getBytes(StandardCharsets.UTF_8);
        this.decl = null;
    }

    public Import(String importModule, String importName, @NotNull Instruction decl) {
        super(InstructionName.IMPORT);
        this.importModule = importModule.getBytes(StandardCharsets.UTF_8);
        this.importName = importName.getBytes(StandardCharsets.UTF_8);
        this.decl = decl;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName()).append(' ');
        Instruction.watWriteBytes(b, importModule);
        b.append(' ');
        Instruction.watWriteBytes(b, importName);
        if (decl != null)
            b.append(' ').append(decl);

        b.append(')');
        return b.toString();
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        assert decl != null;
        assert decl instanceof Func;
        ectx.registerImportFunction(new String(importName, StandardCharsets.UTF_8));
    }
}
