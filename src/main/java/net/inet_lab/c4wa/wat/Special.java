package net.inet_lab.c4wa.wat;

public class Special extends Instruction {
    final String ref;
    final NumType numType;

    public Special(String ref) {
        super(InstructionName.SPECIAL);
        this.ref = ref;
        numType = null;
    }

    Special(NumType numType) {
        super(InstructionName.SPECIAL);
        ref = null;
        this.numType = numType;
    }

    @Override
    public String toString() {
        if (ref != null)
            return "$" + ref;
        else {
            assert numType != null;
            return numType.toString();
        }
    }
}
