package net.inet_lab.c4wa.wat;

public class Local extends Instruction {
    final String ref;
    final NumType numType;

    public Local(String ref, NumType numType) {
        super(InstructionName.LOCAL);
        this.ref = ref;
        this.numType = numType;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " $" + ref + " " + numType + ")";
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        ExecutionFunc f = ectx.getCurrentFunc();
        f.registerLocal(ref, numType);
    }
}
