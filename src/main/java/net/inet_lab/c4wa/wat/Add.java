package net.inet_lab.c4wa.wat;

public class Add extends Expression_2 {
    final public NumType numType;
    public Add(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.ADD, numType, arg1, arg2, Long::sum, Double::sum);
        this.numType = numType;
    }
}
