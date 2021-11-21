package net.inet_lab.c4wa.wat;

public class Instruction_3 extends Instruction {
    final public Expression arg1;
    final public Expression arg2;
    final public Expression arg3;

    public Instruction_3(InstructionType type, Expression arg1, Expression arg2, Expression arg3) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + arg1 + " " + arg2 + " " + arg3 + ")";
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new Instruction_3(type, arg1.postprocess(ppctx), arg2.postprocess(ppctx), arg3.postprocess(ppctx))};
    }
}
