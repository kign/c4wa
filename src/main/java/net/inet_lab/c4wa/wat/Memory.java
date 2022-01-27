package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.Nullable;

public class Memory extends Instruction {
    final @Nullable Export export;
    final @Nullable Import anImport;
    final int pages;

    public Memory(String export_name, int pages) {
        super(InstructionName.MEMORY);
        this.export = new Export(export_name);
        this.anImport = null;
        this.pages = pages;
    }

    public Memory(String import_module, String import_name, int pages) {
        super(InstructionName.MEMORY);
        this.export = null;
        this.anImport = new Import(import_module, import_name);
        this.pages = pages;
    }

    public Memory(int pages) {
        super(InstructionName.MEMORY);
        this.export = null;
        this.anImport = null;
        this.pages = pages;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName());
        if (export != null)
            b.append(' ').append(export);
        if (anImport != null)
            b.append(' ').append(anImport);
        b.append(' ').append(pages);
        b.append(')');
        return b.toString();
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        if (anImport != null)
            throw new RuntimeException("Import memory is not supported by interpreter");
        ectx.initMemory(pages);
    }
}
