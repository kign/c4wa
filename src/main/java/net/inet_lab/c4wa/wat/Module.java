package net.inet_lab.c4wa.wat;

import java.util.List;

public class Module extends Instruction_list {
    public Module(List<Instruction> elements) {
        super(InstructionName.MODULE, elements.toArray(Instruction[]::new), true);
    }
}
