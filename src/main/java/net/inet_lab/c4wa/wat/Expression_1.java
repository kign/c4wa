package net.inet_lab.c4wa.wat;

public class Expression_1 extends Expression {
    final Expression arg;
    Expression_1(InstructionName name, NumType numType, Expression arg) {
        super(name, numType);
        this.arg = arg.comptime_eval();
    }

    @Override
    public String toString() {
        return "(" + fullName() + " " + arg + ")";
    }
}
