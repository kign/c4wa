package net.inet_lab.c4wa.wat;

public class Return extends Instruction_Return {
    public Return(Instruction arg) {
        super(InstructionName.RETURN, arg);
    }
    public Return() {
        this(null);
    }
}
