package net.inet_lab.c4wa.wat;

import java.util.List;

public class Func extends Instruction_list {
    public Func(List<Instruction> attributes, List<Instruction> elements) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), elements.toArray(Instruction[]::new));
    }

    public Func(List<Instruction> attributes) {
        super(InstructionName.FUNC, attributes.toArray(Instruction[]::new), false);
    }
}
