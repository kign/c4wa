package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Global extends Instruction {
    final @NotNull String ref;
    final @NotNull NumType numType;
    final @Nullable Export export;
    final @Nullable Import anImport;
    final @Nullable Expression initialization;
    final boolean mutable;

    public Global(@NotNull String ref, String import_mod, String import_name, @NotNull NumType numType, boolean mutable) {
        super(InstructionName.GLOBAL);
        this.ref = ref;
        this.numType = numType;
        this.export = null;
        this.anImport = new Import(import_mod, import_name);
        this.initialization = null;
        this.mutable = mutable;
    }

    public Global(@NotNull String ref, String export_name, @NotNull NumType numType, boolean mutable, @NotNull Expression initialValue) {
        super(InstructionName.GLOBAL);
        this.ref = ref;
        this.numType = numType;
        this.export = new Export(export_name);
        this.anImport = null;
        this.initialization = initialValue;
        this.mutable = mutable;
    }

    public Global(@NotNull String ref, @NotNull NumType numType, boolean mutable, @NotNull Expression initialValue) {
        super(InstructionName.GLOBAL);
        this.ref = ref;
        this.numType = numType;
        this.export = null;
        this.anImport = null;
        this.initialization = initialValue;
        this.mutable = mutable;
    }

    public Global(@NotNull String ref, @NotNull NumType numType, @Nullable Export export, boolean mutable, @NotNull Expression initialValue) {
        super(InstructionName.GLOBAL);
        this.ref = ref;
        this.numType = numType;
        this.export = export;
        this.anImport = null;
        this.initialization = initialValue;
        this.mutable = mutable;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName()).append(" $").append(ref);
        if (export != null)
            b.append(' ').append(export);
        if (anImport != null)
            b.append(' ').append(anImport);

        if (mutable)
            b.append(" (mut ").append(numType).append(')');
        else
            b.append(' ').append(numType);

        if (initialization != null)
            b.append(' ').append(initialization);

        b.append(')');
        return b.toString();
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        if (initialization == null)
            return new Instruction[]{this};

        Expression pp_ini = initialization.postprocess(ppctx);
        if (pp_ini == initialization)
            return new Instruction[]{this};

        assert anImport == null;
        return new Instruction[]{new Global(ref, numType, export, mutable, pp_ini)};
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        if (anImport != null)
            throw new RuntimeException("imported globals not supported in the interpreter");
        assert this.initialization != null;
        assert this.initialization instanceof Const;
        ectx.registerGlobal(ref, mutable, (Const) this.initialization);
    }
}
