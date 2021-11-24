package net.inet_lab.c4wa.wat;

abstract public class Expression {
    final InstructionName name;
    final NumType numType;

    Expression(InstructionName name, NumType numType) {
        this.name = name;
        this.numType = numType;
    }

    String fullName() {
        if (numType == null)
            return name.getName();
        else
            return numType.name + "." + name.getName();
    }

    abstract public String toString();

    public int complexity() {
        return 1000;
    }

    public Expression comptime_eval() {
        return this;
    }

    public Expression postprocess(PostprocessContext ppctx) {
        return this;
    }

    public Expression Not(NumType numType) {
        if (numType == NumType.I32 || numType == NumType.I64)
            return new Eqz(numType, this);
        else
            throw new RuntimeException("Cannot take logical negative of '" + fullName() + "'");
    }
}
