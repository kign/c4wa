package net.inet_lab.c4wa.wat;

import java.nio.charset.StandardCharsets;

public class Export extends Instruction_Decl {
    public Export(String name) {
        super(InstructionName.EXPORT, new Special(name.getBytes(StandardCharsets.UTF_8)));
    }
}
