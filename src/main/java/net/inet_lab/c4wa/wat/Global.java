package net.inet_lab.c4wa.wat;

public class Global extends Instruction_Decl {
    Global(String ref, String import_mod, String import_name, NumType numType) {
        super(InstructionName.GLOBAL, new Special(ref), new Import(import_mod, import_name), new Special(numType));
    }

    Global(String ref, String export_name, NumType numType) {
        super(InstructionName.GLOBAL, new Special(ref), new Export(export_name), new Special(numType));
    }
}
