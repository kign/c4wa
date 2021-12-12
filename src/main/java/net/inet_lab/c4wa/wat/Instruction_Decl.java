package net.inet_lab.c4wa.wat;

public class Instruction_Decl extends Instruction {
    final Instruction i1;
    final Expression  e1;
    final Instruction i2;
    final Instruction i3;
    final Expression  e3;

    Instruction_Decl(InstructionName type, Instruction i1) {
        super(type);
        this.i1 = i1;
        e1 = null;
        i2 = null;
        i3 = null;
        e3 = null;
    }

    Instruction_Decl(InstructionName type, Instruction i1, Instruction i2) {
        super(type);
        this.i1 = i1;
        this.i2 = i2;
        e1 = null;
        i3 = null;
        e3 = null;
    }

    Instruction_Decl(InstructionName type, Expression e1, Instruction i2) {
        super(type);
        this.e1 = e1;
        this.i2 = i2;
        i1 = null;
        i3 = null;
        e3 = null;
    }

    Instruction_Decl(InstructionName type, Instruction i1, Instruction i2, Instruction i3) {
        super(type);
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        e1 = null;
        e3 = null;
    }

    Instruction_Decl(InstructionName type, Instruction i1, Instruction i2, Expression e3) {
        super(type);
        this.i1 = i1;
        this.i2 = i2;
        this.e3 = e3;
        e1 = null;
        i3 = null;
    }

    Instruction_Decl(InstructionName type, Instruction i1, Instruction i2, Instruction i3, Expression e3) {
        super(type);
        e1 = null;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.e3 = e3;
    }

    Instruction_Decl(InstructionName type, Expression e1, Instruction i1, Instruction i2, Instruction i3, Expression e3) {
        super(type);
        this.e1 = e1;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.e3 = e3;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("(").append(type.getName());
        if (i1 != null)
            b.append(' ').append(i1);
        if (e1 != null)
            b.append(' ').append(e1);
        if (i2 != null)
            b.append(' ').append(i2);
        if (i3 != null)
            b.append(' ').append(i3);
        if (e3 != null)
            b.append(' ').append(e3);
        b.append(")");
        return b.toString();
    }

    public Instruction[] postprocess(PostprocessContext ppctx) {
        return new Instruction[]{new Instruction_Decl((InstructionName) type, e1 == null? null: e1.postprocess(ppctx), i1, i2, i3, e3 == null? null: e3.postprocess(ppctx))};
    }


}
