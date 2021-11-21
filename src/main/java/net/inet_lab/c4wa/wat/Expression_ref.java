package net.inet_lab.c4wa.wat;

abstract public class Expression_ref extends Expression {
    final String ref;

    Expression_ref(InstructionName name, NumType numType, String ref) {
        super(name, numType);
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "(" + name.getName() + " $" + ref + ")";
    }

    @Override
    public int complexity() {
        return 1;
    }
}
