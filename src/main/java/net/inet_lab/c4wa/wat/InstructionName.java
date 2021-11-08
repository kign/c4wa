package net.inet_lab.c4wa.wat;

public enum InstructionName implements InstructionType {
    ADD ("add"),
    SUB ("sub"),
    NEG ("neg"),
    MUL ("mul"),
    DIV ("div"),
    DIV_U ("div_u"),
    DIV_S ("div_s"),
    REM ("rem"),
    REM_U ("rem_u"),
    REM_S ("rem_s"),
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
    EQZ ("eqz"),
    EQ ("eq"),
    NE ("ne"),
    AND ("and"),
    OR ("or"),
    GET_LOCAL ("get_local"),
    SET_LOCAL ("set_local"),
    TEE_LOCAL ("tee_local"),
    GET_GLOBAL ("global.get"),
    SET_GLOBAL ("global.set"),
    CALL ("call"),
    STORE ("store"),
    CONST ("const"),
    LOOP ("loop"),
    BLOCK ("block"),
    BR_IF ("br_if"),
    BR ("br"),
    IF ("if"),
    THEN ("then"),
    ELSE ("else"),
    RETURN ("return"),

    WRAP_I64 ("wrap_i64"),
    EXTEND_I32_S ("extend_i32_s"),
    EXTEND_I32_U ("extend_i32_u"),
    TRUNC_F32_S ("trunc_f32_s"),
    TRUNC_F32_U ("trunc_f32_u"),
    TRUNC_F64_S ("trunc_f64_s"),
    TRUNC_F64_U ("trunc_f64_u"),
    DEMOTE_F64 ("demote_f64"),
    PROMOTE_F32 ("promote_f32"),
    CONVERT_I32_S ("convert_i32_s"),
    CONVERT_I64_S ("convert_i64_s"),
    CONVERT_I32_U ("convert_i32_u"),
    CONVERT_I64_U ("convert_i64_u"),
    REINTERPRET_F32 ("reinterpret_f32"),
    REINTERPRET_F64 ("reinterpret_f64"),
    REINTERPRET_I32 ("reinterpret_i32"),
    REINTERPRET_I64 ("reinterpret_i64"),

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

    @Override
    public NumType getPrefix() {
        return null;
    }

    @Override
    public InstructionName getMain() {
        return this;
    }
}
