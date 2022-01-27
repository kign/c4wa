package net.inet_lab.c4wa.wat;

public class Shift extends Expression_2 {
    public Shift(NumType numType, boolean isLeft, boolean isSigned, Expression arg1, Expression arg2) {
        super(isLeft? InstructionName.SHL: (isSigned? InstructionName.SHR_S: InstructionName.SHR_U), numType,
                arg1, arg2, new IConstShift(numType, isLeft, isSigned),
                null);
    }

    private static class IConstShift implements Const.TwoArgIntOperator {
        final NumType numType;
        final boolean isLeft;
        final boolean isSigned;

        IConstShift(NumType numType, boolean isLeft, boolean isSigned) {
            this.numType = numType;
            this.isLeft = isLeft;
            this.isSigned = isSigned;
        }

        @Override
        public long op(long a, long b) {
            if (isLeft) {
                if (numType == NumType.I32) {
                    int res = (int) a << (int) b;
                    if (isSigned)
                        return res;
                    else
                        return Integer.toUnsignedLong(res);
                }
                else
                    return a << b;
            }
            else {
                if (numType == NumType.I32) {
                    if (isSigned)
                        return (int)a >> (int) b;
                    else
                        return Integer.toUnsignedLong((int) a >>> (int) b);
                }
                else {
                    if (isSigned)
                        return a >> b;
                    else
                        return a >>> b;
                }
            }
        }
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Shift(numType, name == InstructionName.SHL, name == InstructionName.SHR_S,
                arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
