package net.inet_lab.c4wa.wat;

public class Drop extends Instruction_1 {
    public Drop(Expression arg) {
        super(InstructionName.DROP, arg);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new Drop(arg.postprocess(ppctx))};
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        arg.eval(ectx);
    }
}
