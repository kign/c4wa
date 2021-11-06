package net.inet_lab.c4wa.wat;

public class Result extends Instruction_Decl {
    public Result(NumType numType) {
        super(InstructionName.RESULT, new Special(numType));
    }
}
