package net.inet_lab.c4wa.wat;

public class GetLocal extends Instruction_GetLocal {
    public GetLocal(String ref) {
        super(InstructionName.GET_LOCAL, ref);
    }
}
