package net.inet_lab.c4wa.wat;

public enum InstructionName implements InstructionType {
    ADD ("add"),
    SUB ("sub"),
    MUL ("mul"),
    LT_S ("lt_s"),
    LT_U ("lt_u"),
    LE_S ("le_s"),
    LE_U ("le_u"),
    GT_S ("gt_s"),
    GT_U ("gt_u"),
    GE_S ("ge_s"),
    GE_U ("ge_u"),
    LT ("lt"),
    LE ("le"),
    GT ("gt"),
    GE ("ge"),
    GET_LOCAL ("get_local"),
    SET_LOCAL ("set_local"),
    TEE_LOCAL ("tee_local"),
    GET_GLOBAL ("global_get"),
    SET_GLOBAL ("global_set"),
    CALL ("call"),
    STORE ("store"),
    CONST ("const"),
    LOOP ("loop"),
    BLOCK ("block"),
    BR_IF ("br_if"),
    RETURN ("return"),

    EXPORT ("export"),
    IMPORT ("import"),
    LOCAL ("local"),
    GLOBAL ("global"),
    FUNC ("func"),
    MODULE ("module"),
    RESULT ("result"),
    PARAM ("param"),
    MEMORY ("memory"),
    DATA ("data"),
    MUT ("mut"),

    SPECIAL("<special>"); // fake

    private final String name;

    InstructionName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
