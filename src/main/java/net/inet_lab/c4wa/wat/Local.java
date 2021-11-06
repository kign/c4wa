package net.inet_lab.c4wa.wat;

public class Local extends Instruction_Decl {
    public Local(String ref, NumType numType) {
        super(InstructionName.LOCAL, new Special(ref), new Special(numType));
    }
}
