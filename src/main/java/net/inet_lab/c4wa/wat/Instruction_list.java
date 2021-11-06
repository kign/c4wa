package net.inet_lab.c4wa.wat;

abstract public class Instruction_list extends Instruction {
    final Instruction[] elements;
    public Instruction_list(InstructionType type, Instruction[] elements) {
        super(type);
        this.elements = elements;
    }

    abstract String detailsToString();

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName()).append(' ').append(detailsToString()).append('\n');
        for (int i = 0; i < elements.length; i ++) {
            b.append(elements[i]);
            if (i < elements.length - 1)
                b.append('\n');
            else
                b.append(')');
        }
        return b.toString();
    }
}
