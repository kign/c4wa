package net.inet_lab.c4wa.wat;

public class Rem extends Expression_2 {
    public Rem(NumType numType, boolean signed, Expression arg1, Expression arg2) {
        super(signed ? InstructionName.REM_S : InstructionName.REM_U,
                numType, arg1, arg2, new IRemEval(signed),null);
    }

    private static class IRemEval implements Const.TwoArgIntOperator {
        final boolean signed;

        IRemEval(boolean signed) {
            this.signed = signed;
        }

        @Override
        public long op(long a, long b) {
            if (signed)
                return a % b;
            else
                return Long.remainderUnsigned(a, b);
        }
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Rem(numType, name == InstructionName.REM_S, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
