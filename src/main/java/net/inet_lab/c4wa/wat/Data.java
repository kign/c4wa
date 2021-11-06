package net.inet_lab.c4wa.wat;

public class Data extends Instruction_Decl {
    public Data(int offset, byte[] data, int data_len) {
        super(InstructionName.DATA, new Const(offset), new Special(data, data_len));
    }
}
