package net.inet_lab.c4wa.wat;

public class Expression_2ref extends Expression {
    final String ref;
    final Expression arg1;
    final Expression arg2;

    public Expression_2ref(InstructionName name, NumType numType, String ref, Expression arg1, Expression arg2) {
        super(name, numType);
        this.ref = ref;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return "(" + name.getName() + " $" + ref + " " + arg1 + " " + arg2 + ")";
    }

    @Override
    public int complexity() {
        return 1 + Math.max(arg1.complexity(), arg2.complexity());
    }

}
