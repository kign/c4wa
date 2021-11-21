package net.inet_lab.c4wa.wat;

abstract public class Expression_Delayed extends Expression {
    public Expression_Delayed() {
        super(InstructionName.SPECIAL, NumType.I32);
    }
}
