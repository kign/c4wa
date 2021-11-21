package net.inet_lab.c4wa.wat;

public class Mul extends Expression_2 {
    public Mul(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.MUL, numType, arg1, arg2, (a,b)->a*b, (a,b)->a*b);
    }
}
