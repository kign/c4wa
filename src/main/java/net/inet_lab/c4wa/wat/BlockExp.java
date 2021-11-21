package net.inet_lab.c4wa.wat;

public class BlockExp extends Expression {
    final String ref;
    final Instruction[] body;
    final Expression returnExp;

    public BlockExp(String ref, NumType numType, Instruction[] body, Expression returnExp) {
        super(InstructionName.BLOCK, numType);
        this.ref = ref;
        this.body = body;
        this.returnExp = returnExp;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append('(').append(name.getName()).append(" $").append(ref).append(" (result ").append(numType.name).append(')');
        for (var i: body)
            b.append(' ').append(i);
        b.append(' ').append(returnExp).append(')');

        return b.toString();
    }
}
