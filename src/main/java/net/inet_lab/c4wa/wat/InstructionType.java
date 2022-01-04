package net.inet_lab.c4wa.wat;

public interface InstructionType extends WasmOutputStream.Opcode {
    String getName ();
    NumType getPrefix();
    InstructionName getMain();
}
