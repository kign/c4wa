package net.inet_lab.c4wa.wat;

public class Block extends Instruction_list {
    public Block(String ref, Instruction[] elements) {
        super(InstructionName.BLOCK, ref, elements);
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        try {
            super.execute(ectx);
        } catch (ExecutionFunc.ExeBreak exeBreak) {
            assert ref != null;
            if (!ref.equals(exeBreak.label))
                throw exeBreak;
        }
    }
}
