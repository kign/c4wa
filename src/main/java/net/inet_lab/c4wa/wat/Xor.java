package net.inet_lab.c4wa.wat;

public class Xor extends Expression_2 {
    final public NumType numType;

    public Xor(NumType numType, Expression arg1, Expression arg2) {
        super(InstructionName.XOR, numType, arg1, arg2, (a,b)->a^b,null);
        this.numType = numType;
    }

    @Override
    public Expression postprocess(PostprocessContext ppctx) {
        return new Xor(numType, arg1.postprocess(ppctx), arg2.postprocess(ppctx));
    }
}
