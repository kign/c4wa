package net.inet_lab.c4wa.wat;

public class SetLocal extends Instruction_SetLocal {
    public SetLocal(String ref, Instruction arg) {
        super(InstructionName.SET_LOCAL, ref, arg);
    }
}
