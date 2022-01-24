package net.inet_lab.c4wa.wat;

public class MinMax extends Expression_2 {
    public MinMax(NumType numType, boolean isMin, Expression arg1, Expression arg2) {
        super(isMin? InstructionName.MIN: InstructionName.MAX, numType, arg1, arg2,
                null, isMin? Math::min: Math::max);
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new MinMax(numType, name == InstructionName.MIN, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
