package net.inet_lab.c4wa.wat;

public class Loop extends Instruction_list {
    public Loop(String ref, Instruction[] elements) {
        super(InstructionName.LOOP, ref, elements);
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        while (true) {
            try {
                super.execute(ectx);
            } catch (ExecutionFunc.ExeBreak exeBreak) {
                assert ref != null;
                if (ref.equals(exeBreak.label)) {
                    continue;
                }
                else
                    throw  exeBreak;
            }
            break;
        }
    }
}
