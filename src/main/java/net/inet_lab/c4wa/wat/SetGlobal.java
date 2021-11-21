package net.inet_lab.c4wa.wat;

public class SetGlobal extends Instruction_1ref {
    public SetGlobal(String ref, Expression arg) {
        super(InstructionName.SET_GLOBAL, ref, arg);
    }
}
