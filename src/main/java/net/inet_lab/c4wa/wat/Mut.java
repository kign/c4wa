package net.inet_lab.c4wa.wat;

public class Mut extends Instruction_Decl {
    Mut(NumType numType) {
        super(InstructionName.MUT, new Special(numType));
    }
}
