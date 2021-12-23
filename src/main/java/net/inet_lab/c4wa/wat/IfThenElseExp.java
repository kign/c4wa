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

        return '(' + name.getName() +
                ' ' +
                new Result(numType) +
                ' ' + condition +
                " (then " + _then + ") (else " + _else + ')' +
                ')';
    }

    @Override
    public int complexity() {
        return 1 + Math.max(condition.complexity(), Math.max(_then.complexity(), _else.complexity()));
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new IfThenElseExp(condition.postprocess(ppctx), numType, _then.postprocess(ppctx), _else.postprocess(ppctx));
    }
}