package net.inet_lab.c4wa.wat;

public class Call extends Instruction {
    public final String name;
    public final Instruction[] args;
    public Call(String name, Instruction[] args) {
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
}
