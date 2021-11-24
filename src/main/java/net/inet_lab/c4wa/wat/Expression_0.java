package net.inet_lab.c4wa.wat;

public class Expression_0 extends Expression {
    Expression_0(InstructionName name) {
        super(name, null);
    }

    @Override
    public String toString() {
        return "(" + name.getName() + ")";
    }

    public int complexity() {
        return 1;
    }

}
