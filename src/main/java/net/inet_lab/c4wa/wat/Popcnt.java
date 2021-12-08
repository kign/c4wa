package net.inet_lab.c4wa.wat;

public class Popcnt extends Expression_1 {
    public Popcnt(NumType numType, Expression arg) {
        super(InstructionName.POPCNT, numType, arg);
    }
}
