package net.inet_lab.c4wa.wat;

public class SetLocal extends Instruction_1ref {
    public SetLocal(String ref, Expression arg) {
        super(InstructionName.SET_LOCAL, ref, arg);
    }
}
