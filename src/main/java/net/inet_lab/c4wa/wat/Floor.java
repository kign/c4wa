package net.inet_lab.c4wa.wat;

public class Floor extends Expression_1 {
    public Floor(NumType numType, Expression arg) {
        super(InstructionName.FLOOR, numType, arg);
    }
}
