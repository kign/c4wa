package net.inet_lab.c4wa.wat;

public class Rem extends Expression_2 {
    public Rem(NumType numType, boolean signed, Expression arg1, Expression arg2) {
        super(signed ? InstructionName.REM_S : InstructionName.REM_U,
                numType, arg1, arg2, null,null);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Rem(numType, name == InstructionName.REM_S, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
