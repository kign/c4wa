package net.inet_lab.c4wa.wat;

public class Xor extends Expression_2 {
    final public NumType numType;

    public Xor(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.XOR, numType, arg1, arg2, (a,b)->a^b,null);
        this.numType = numType;
    }
}
