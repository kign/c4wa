package net.inet_lab.c4wa.wat;

public interface InstructionType {
    String getName ();
    NumType getPrefix();
    InstructionName getMain();
}
