package net.inet_lab.c4wa.wat;

public class Global extends Instruction_Decl {
    public Global(String ref, String import_mod, String import_name, NumType numType, boolean mutable) {
        super(InstructionName.GLOBAL, new Special(ref), new Import(import_mod, import_name),
                mutable?new Mut(numType): new Special(numType));
    }

    public Global(String ref, String export_name, NumType numType, boolean mutable, Const initialValue) {
        super(InstructionName.GLOBAL, new Special(ref), new Export(export_name),
                mutable ? new Mut(numType) : new Special(numType), initialValue);
    }

    public Global(String ref, NumType numType, boolean mutable, Const initialValue) {
        super(InstructionName.GLOBAL, new Special(ref),
                mutable ? new Mut(numType) : new Special(numType), initialValue);
    }
}
