package net.inet_lab.c4wa.wat;

public class Result extends Instruction {
    final NumType numType;
    public Result(NumType numType) {
        super(InstructionName.RESULT);
        this.numType = numType;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + numType + ")";
    }
}
