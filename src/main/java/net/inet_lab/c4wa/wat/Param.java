package net.inet_lab.c4wa.wat;

public class Param extends Instruction_Decl {
    public Param(String ref, NumType numType) {
        super(InstructionName.PARAM, new Special(ref), new Special(numType));
    }

    public Param(NumType numType) {
        super(InstructionName.PARAM, null, new Special(numType));
    }
}
