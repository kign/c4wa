package net.inet_lab.c4wa.wat;

public class SetLocal extends Instruction_1ref {
    public SetLocal(String ref, Expression arg) {
        super(InstructionName.SET_LOCAL, ref, arg);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new SetLocal(ref, arg.postprocess(ppctx))};
    }
}
