package net.inet_lab.c4wa.wat;

import java.nio.charset.StandardCharsets;

public class Import extends Instruction_Decl {
    public Import(String mod, String name) {
        super(InstructionName.IMPORT,
                new Special(mod.getBytes(StandardCharsets.UTF_8)),
                new Special(name.getBytes(StandardCharsets.UTF_8)));
    }

    public Import(String mod, String name, Instruction decl) {
        super(InstructionName.IMPORT,
                new Special(mod.getBytes(StandardCharsets.UTF_8)),
                new Special(name.getBytes(StandardCharsets.UTF_8)),
                decl);
    }
}
