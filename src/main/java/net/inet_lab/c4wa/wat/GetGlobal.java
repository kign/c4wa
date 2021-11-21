package net.inet_lab.c4wa.wat;

public class GetGlobal extends Expression_ref {
    public GetGlobal(NumType numType, String ref) {
        super(InstructionName.GET_GLOBAL, numType, ref);
    }
}
