package net.inet_lab.c4wa.wat;

import java.util.Arrays;

public class Call extends Instruction {
    public final String name;
    public final Expression[] args;

    public Call(String name, Expression[] args) {
        super(InstructionName.CALL);
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("(call $").append(name);
        for (var arg: args)
            b.append(" ").append(arg);
        b.append(")");
        return b.toString();
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new Call(name, Arrays.stream(args).map(e -> e.postprocess(ppctx)).toArray(Expression[]::new))};
    }
}
