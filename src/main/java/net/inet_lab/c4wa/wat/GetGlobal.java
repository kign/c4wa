package net.inet_lab.c4wa.wat;

public class GetGlobal extends Instruction_GetLocal {
    public GetGlobal(String ref) {
        super(InstructionName.GET_GLOBAL, ref);
    }
}
