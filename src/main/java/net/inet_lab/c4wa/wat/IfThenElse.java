package net.inet_lab.c4wa.wat;

import org.jetbrains.annotations.NotNull;

public class IfThenElse extends Instruction {
    final Expression condition;
    final Instruction_list _then;
    final Instruction_list _else;

    public IfThenElse(Expression condition, @NotNull Instruction[] thenList, Instruction[] elseList) {
        super(InstructionName.IF);
        this.condition = condition;
        this._then = (thenList == null)? null : new Then(thenList);
        this._else = (elseList == null)? null : new Else(elseList);
    }

    private IfThenElse(Expression condition, Instruction_list _then, Instruction_list _else) {
        super(InstructionName.IF);
        this.condition = condition;
        this._then = _then;
        this._else = _else;
    }
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new IfThenElse(condition.postprocess(ppctx), _then == null? null : _then.postprocessList(ppctx),
                _else == null? null: _else.postprocessList(ppctx))};
    }

    @Override
    public String toStringPretty(int indent) {
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