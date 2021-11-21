package net.inet_lab.c4wa.wat;

public class GetLocal extends Expression_ref {
    public GetLocal(NumType numType, String ref) {
        super(InstructionName.GET_LOCAL, numType, ref);
    }
}
