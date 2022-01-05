package net.inet_lab.c4wa.wat;

public class Store extends Instruction_2 {
    final public NumType numType;

    public Store(NumType numType, Expression offset, Expression value) {
        super(new InstructionWithNumPrefix(numType, InstructionName.STORE), offset, value);
        this.numType = numType;
    }

    public Store(NumType numType, int wrap, Expression offset, Expression value) {
        super(new InstructionWithNumPrefix(numType,
                (wrap == 8)? InstructionName.STORE8 : ((wrap == 16) ? InstructionName.STORE16 : InstructionName.STORE32)),
                offset, value);
        this.numType = numType;
    }

/*
    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        Expression offset = arg1.postprocess(ppctx);
        Expression value = arg2.postprocess(ppctx);
        Store store = type.getMain() == InstructionName.STORE8 ? new Store(numType, 8, offset, value)
                      : type.getMain() == InstructionName.STORE16 ? new Store(numType, 16, offset, value)
                      : new Store(numType, offset, value);
        return new Instruction[]{store};
    }
*/

}
