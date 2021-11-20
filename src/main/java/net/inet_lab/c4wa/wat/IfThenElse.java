package net.inet_lab.c4wa.wat;

public class IfThenElse extends Instruction {
    final Instruction condition;
    final NumType resultType;
    final Instruction_list _then;
    final Instruction_list _else;

    public IfThenElse(Instruction condition, NumType resultType, Instruction[] thenList, Instruction[] elseList) {
        super(InstructionName.IF);
        this.condition = condition;
        this._then = (thenList == null)? null : new Then(thenList);
        this._else = (elseList == null)? null : new Else(elseList);
        this.resultType = resultType;
    }

    private IfThenElse(Instruction condition, NumType resultType, Instruction_list _then, Instruction_list _else) {
        super(InstructionName.IF);
        this.condition = condition;
        this.resultType = resultType;
        this._then = _then;
        this._else = _else;
    }
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new IfThenElse(condition, resultType, _then == null? null : (Instruction_list) _then.postprocess(ppctx)[0],
                _else == null? null: (Instruction_list) _else.postprocess(ppctx)[0])};
    }

    @Override
    public String toStringPretty(int indent) {
        if (resultType != null)
            return toString();

        StringBuilder b = new StringBuilder();

        b.append('(').append(type.getName())
                .append(' ')
                .append(condition.toStringPretty(indent+2))
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
        StringBuilder b = new StringBuilder();

        b.append('(').append(type.getName())
                .append(' ');

        if (resultType != null)
            b.append(new Result(resultType));

        b.append(' ').append(condition);

        if (resultType == null) {
            if (_then != null)
                b.append(_then);
            if (_else != null)
                b.append(_else);
        }
        else
            b.append(" (then ").append(_then.elements[0]).append(") (else ").append(_else.elements[0]).append(')');
        b.append(')');

        return b.toString();
    }

    @Override
    public int complexity() {
        int ret = condition.complexity();

        if (_then != null && _then.complexity() > ret)
            ret = _then.complexity();
        if (_else != null && _else.complexity() > ret)
            ret = _else.complexity();

        return ret + 1;
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