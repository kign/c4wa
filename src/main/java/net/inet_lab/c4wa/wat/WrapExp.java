package net.inet_lab.c4wa.wat;

public class WrapExp extends Instruction_1 {
    public WrapExp(Expression arg) {
        super(InstructionName.SPECIAL, arg);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        Expression a = arg.postprocess(ppctx);
        if (a == arg)
            return new Instruction[]{this};
        else
            return new Instruction[]{new WrapExp(a)};
    }

    @Override
    public String toString() {
        return arg.toString();
    }
}
