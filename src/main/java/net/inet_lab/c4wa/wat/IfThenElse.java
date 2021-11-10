package net.inet_lab.c4wa.wat;

import java.util.Enumeration;

public class IfThenElse extends Instruction {
    final Instruction condition;
    final NumType resultType;
    final Then _then;
    final Else _else;

    public IfThenElse(Instruction condition, NumType resultType, Instruction[] thenList, Instruction[] elseList) {
        super(InstructionName.IF);
        this.condition = condition;
        this._then = (thenList == null)? null : new Then(thenList);
        this._else = (elseList == null)? null : new Else(elseList);
        this.resultType = resultType;
    }

    @Override
    public String toStringPretty(int indent) {
        if (resultType != null)
            return toString();

        StringBuilder b = new StringBuilder();

        b.append('(').append(type.getName())
                .append(' ')
                .append(condition)
                .append('\n');

        if (_then != null)
            b.append(" ".repeat(indent)).append(_then.toStringPretty(indent + 2));
        if (_else != null)
            b.append('\n').append(" ".repeat(indent)).append(_else.toStringPretty(indent + 2));
        b.append(')');

        return b.toString();
    }

    @Override
    public String toString() {
        return '(' + type.getName() +
                " (result " +
                resultType +
                ") " +
                condition +
                " (then " +
                _then.elements[0] +
                ") (else " +
                _else.elements[0] +
                "))";
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