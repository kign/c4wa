package net.inet_lab.c4wa.wat;

public class Memory extends Instruction_Decl {
    public Memory(String export_name, int pages) {
        super(InstructionName.MEMORY, new Export(export_name), new Special(pages));
    }
}
