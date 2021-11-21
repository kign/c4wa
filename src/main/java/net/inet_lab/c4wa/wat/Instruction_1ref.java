package net.inet_lab.c4wa.wat;

public class Instruction_1ref extends Instruction {
    public final String ref;
    final public Expression arg;

    public Instruction_1ref(InstructionType type, String ref, Expression arg) {
        super(type);
        this.ref = ref;
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + arg +")";
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        if (arg == null)
            return new Instruction[]{this};
        else
            return new Instruction[]{new Instruction_1ref(type, ref, arg.postprocess(ppctx))};
    }

}
