package net.inet_lab.c4wa.wat;

public class BrIf extends Instruction {
    final String ref;
    final Instruction condition;
    final Instruction returnValue;

    public BrIf(String ref, Instruction condition) {
        this(ref, condition, null);
    }

    public BrIf(String ref, Instruction condition, Instruction returnValue) {
        super(InstructionName.BR_IF);
        this.ref = ref;
        this.condition = condition;
        this.returnValue = returnValue;
    }

    @Override
    public String toStringPretty(int indent) {
        StringBuilder b = new StringBuilder();

        b.append("(").append(type.getName()).append(" $").append(ref);

        if (returnValue != null)
            b.append(" ").append(returnValue.toStringPretty(indent));

        b.append(" ").append(condition.toStringPretty(indent)).append(")");

        return b.toString();
    }

    @Override
    public String toString() {
        return toStringPretty(0);
    }
}
