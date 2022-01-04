package net.inet_lab.c4wa.wat;

public class InstructionWithNumPrefix implements InstructionType {
    private final NumType numType;
    private final InstructionName main;

    InstructionWithNumPrefix(NumType numType, InstructionName main) {
        this.numType = numType;
        this.main = main;
    }

    @Override
    public String getName() {
        return numType.name + "." + main.getName();
    }

    @Override
    public NumType getPrefix() {
        return numType;
    }

    @Override
    public InstructionName getMain() {
        return main;
    }

    @Override
    public byte opcode() {
        if (numType == null)
            return main.opcode();
        else
            return main.opcode(numType);
    }
}
