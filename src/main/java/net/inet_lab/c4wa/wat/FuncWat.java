package net.inet_lab.c4wa.wat;

public class FuncWat extends Instruction {
    final String name;
    final String code;
    public FuncWat(String name, String code) {
        super(InstructionName.FUNC);
        this.name = name;
        this.code = code;
    }

    @Override
    public String toString() {
        return "(" + type.getName() + " " + "$" + name + " " + code + ")";
    }
}
