package net.inet_lab.c4wa.wat;

public class Instruction_Decl extends Instruction {
    final Instruction arg1;
    final Instruction arg2;
    final Instruction arg3;
    final Instruction arg4;

    Instruction_Decl(InstructionName type, Instruction arg1) {
        super(type);
        this.arg1 = arg1;
        arg2 = null;
        arg3 = null;
        arg4 = null;
    }

    Instruction_Decl(InstructionName type, Instruction arg1, Instruction arg2) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        arg3 = null;
        arg4 = null;
    }

    Instruction_Decl(InstructionName type, Instruction arg1, Instruction arg2, Instruction arg3) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        arg4 = null;
    }

    Instruction_Decl(InstructionName type, Instruction arg1, Instruction arg2, Instruction arg3, Instruction arg4) {
        super(type);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("(").append(type.getName());
        if (arg1 != null)
            b.append(' ').append(arg1);
        if (arg2 != null)
            b.append(' ').append(arg2);
        if (arg3 != null)
            b.append(' ').append(arg3);
        if (arg4 != null)
            b.append(' ').append(arg4);
        b.append(")");
        return b.toString();
    }

    @Override
    public int complexity() {
        return 0;
    }
}
