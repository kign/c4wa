package net.inet_lab.c4wa.wat;

public class IfThenElseExp extends Expression {
    final Expression condition;
    final Expression _then;
    final Expression _else;

    public IfThenElseExp(Expression condition, NumType resultType, Expression _then, Expression _else) {
        super(InstructionName.IF, resultType);
        this.condition = condition;
        this._then = _then;
        this._else = _else;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append('(').append(name.getName())
                .append(' ');

        b.append(new Result(numType));

        b.append(' ').append(condition);

        b.append(" (then ").append(_then).append(") (else ").append(_else).append(')');
        b.append(')');

        return b.toString();
    }

    @Override
    public int complexity() {
        return 1 + Math.max(condition.complexity(), Math.max(_then.complexity(), _else.complexity()));
    }
}