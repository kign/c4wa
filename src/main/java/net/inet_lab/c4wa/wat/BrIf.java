package net.inet_lab.c4wa.wat;

public class BrIf extends Instruction_1ref {
//    final String ref;
//    final Expression condition;

    public BrIf(String ref, Expression condition) {
        super(InstructionName.BR_IF, ref, condition);
//        this.ref = ref;
//        this.condition = condition;
    }

//    @Override
//    public String toStringPretty(int indent) {
//        StringBuilder b = new StringBuilder();
//
//        b.append("(").append(type.getName()).append(" $").append(ref);
//
//        b.append(" ").append(condition).append(")");
//
//        return b.toString();
//    }
//
//    @Override
//    public String toString() {
//        return toStringPretty(0);
//    }
}
