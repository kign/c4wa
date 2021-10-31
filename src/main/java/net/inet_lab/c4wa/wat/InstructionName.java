package net.inet_lab.c4wa.wat;

public enum InstructionName implements InstructionType {
    ADD ("add"),
    SUB ("sub"),
    GET_LOCAL ("get_local"),
    RETURN ("return");

    private final String name;

    InstructionName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
