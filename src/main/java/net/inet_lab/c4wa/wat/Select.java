package net.inet_lab.c4wa.wat;

public class Select extends Expression_3 {
    public Select(Expression condition, Expression _then, Expression _else) {
        super(InstructionName.SELECT, null, _then, _else, condition);
    }

    @Override
    public Expression comptime_eval() {
        if (arg3 instanceof Const) {
            Const condition = (Const) arg3;

            if (condition.isTrue())
                return arg1;
            else
                return arg2;
        }
        else
            return this;
    }

}
