package net.inet_lab.c4wa.wat;

public class MemorySize extends Expression_0 {
    public MemorySize() {
        super(InstructionName.MEMORY_SIZE);
    }

    @Override
    public Const eval(ExecutionCtx ectx) {
        return new Const(NumType.I32, ectx.mem_pages);
    }
}
