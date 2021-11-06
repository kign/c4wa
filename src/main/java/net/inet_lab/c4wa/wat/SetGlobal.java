package net.inet_lab.c4wa.wat;

public class SetGlobal extends Instruction_SetLocal {
    public SetGlobal(String ref, Instruction arg) {
        super(InstructionName.SET_GLOBAL, ref, arg);
    }
}
