package net.inet_lab.c4wa.wat;

public class Return extends Instruction_1 {
    public Return(Expression arg) {
        super(InstructionName.RETURN, arg);
    }
    public Return() {
        this(null);
    }
}
