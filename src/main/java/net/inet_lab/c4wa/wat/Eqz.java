package net.inet_lab.c4wa.wat;

public class Eqz extends Expression_1 {
    public Eqz(NumType numType, Expression arg) {
        super(InstructionName.EQZ, numType, arg);
    }

    @Override
    public Expression Not(NumType expNumType) {
        if (expNumType != NumType.I32)
            throw new RuntimeException("Invalid EQZ type " + expNumType + ", must always be I32");
        return GenericCast.cast(this.numType, NumType.I32, false, arg);
    }
}
