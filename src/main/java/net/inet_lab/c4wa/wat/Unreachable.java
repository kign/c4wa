package net.inet_lab.c4wa.wat;

public class Unreachable extends Instruction_0 {
    public Unreachable() {
        super(InstructionName.UNREACHABLE);
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        throw new RuntimeException("ABORT");
    }
}
