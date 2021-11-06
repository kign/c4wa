package net.inet_lab.c4wa.wat;

abstract public class Instruction_list extends Instruction {
    final Instruction[] attributes;
    final Instruction[] elements;

    public Instruction_list(InstructionType type, Instruction[] attributes, Instruction[] elements) {
        super(type);
        this.attributes = attributes;
        this.elements = elements;
    }

    public Instruction_list(InstructionType type, String ref, Instruction[] elements) {
        super(type);
        this.attributes = new Instruction[]{new Special(ref)};
        this.elements = elements;
    }

    public Instruction_list(InstructionType type, Instruction[] elements_or_attributes, boolean pElements) {
        super(type);
        this.attributes = pElements? null: elements_or_attributes;
        this.elements = pElements? elements_or_attributes : null;
    }

    @Override
    public String toStringPretty(int indent) {
        StringBuilder b = new StringBuilder();
        b.append('(').append(type.getName());
        if (attributes != null) {
            for (Instruction attribute : attributes)
                b.append(' ').append(attribute);
        }
        if (elements == null)
            b.append(')');
        else {
            b.append('\n');
            for (int i = 0; i < elements.length; i++) {
                b.append(" ".repeat(indent));
                b.append(elements[i].toStringPretty(indent + 2));
                if (i < elements.length - 1)
                    b.append('\n');
                else
                    b.append(')');
            }
        }
        return b.toString();
    }

    @Override
    public String toString() {
        return toStringPretty(0);
    }
}
