package net.inet_lab.c4wa.wat;

public class IfThenElse extends Instruction {
    final Instruction condition;
    final Then _then;
    final Else _else;

    public IfThenElse(Instruction condition, Instruction[] thenList, Instruction[] elseList) {
        super(InstructionName.IF);
        this.condition = condition;
        this._then = (thenList == null)? null : new Then(thenList);
        this._else = (elseList == null)? null : new Else(elseList);
    }

    @Override
    public String toStringPretty(int indent) {
        StringBuilder b = new StringBuilder();

        b.append('(').append(type.getName())
                     .append(' ')
                     .append(condition)
                     .append('\n');

        if (_then != null)
            b.append(" ".repeat(indent + 2)).append(_then.toStringPretty(indent + 4));
        if (_else != null)
            b.append('\n').append(" ".repeat(indent + 2)).append(_else.toStringPretty(indent + 4));
        b.append(')');

        return b.toString();
    }

    @Override
    public String toString() {
        return toStringPretty(0);
    }

    private static class Then extends Instruction_list {
        public Then(Instruction[] elements) {
            super(InstructionName.THEN, elements, true);
        }
    }

    private static class Else extends Instruction_list {
        public Else(Instruction[] elements) {
            super(InstructionName.ELSE, elements, true);
        }
    }
}