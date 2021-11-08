package net.inet_lab.c4wa.wat;

public class Eqz extends Instruction_Return {
    public Eqz(NumType numType, Instruction arg) {
        super(new InstructionWithNumPrefix(numType, InstructionName.EQZ), arg);
    }
}
