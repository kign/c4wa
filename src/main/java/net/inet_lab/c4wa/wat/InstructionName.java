package net.inet_lab.c4wa.wat;

public enum InstructionName implements InstructionType, WasmOutputStream.Opcode {
    ADD ("add", null, 0x6a7c92a0),
    SUB ("sub", null, 0x6b7d92a0),
    NEG ("neg", null, 0x00008c9a),
    MUL ("mul", null, 0x6c7e94a2),
    DIV ("div", null,  0x000095a3),
    DIV_U ("div_u", null, 0x6e800000),
    DIV_S ("div_s", null, 0x6d7f0000),
    REM_U ("rem_u", null, 0x70820000),
    REM_S ("rem_s", null, 0x6f810000),
    MIN ("min", null, 0x000096a4),
    MAX ("max", null, 0x000097a5),
    LT_S ("lt_s", null, 0x48530000),
    LT_U ("lt_u", null, 0x49540000),
    LE_S ("le_s", null, 0x4c570000),
    LE_U ("le_u", null, 0x4d580000),
    GT_S ("gt_s", null, 0x4a550000),
    GT_U ("gt_u", null, 0x4b560000),
    GE_S ("ge_s", null, 0x4e590000),
    GE_U ("ge_u", null, 0x4f5a0000),
    LT ("lt", null, 0x00005d63),
    LE ("le", null, 0x00005f65),
    GT ("gt", null, 0x00005e64),
    GE ("ge", null, 0x00006066),
    EQZ ("eqz", null, 0x45500000),
    EQ ("eq", null, 0x46515b61),
    NE ("ne", null, 0x47525c62),
    AND ("and", null, 0x71830000),
    OR ("or", null, 0x72840000),
    XOR ("xor", null, 0x73850000),
    SHL ("shl", null, 0x74860000),
    SHR_S ("shr_s", null, 0x75870000),
    SHR_U ("shr_u", null, 0x76880000),
    CLZ ("clz", null, 0x67790000),
    CTZ ("ctz", null, 0x687a0000),
    POPCNT ("popcnt", null, 0x697b0000),
    GET_LOCAL ("get_local", 0x20, null),
    SET_LOCAL ("set_local", 0x21, null),
    TEE_LOCAL ("tee_local", 0x22, null),
    GET_GLOBAL ("global.get", 0x23, null),
    SET_GLOBAL ("global.set", 0x24, null),
    CALL ("call", 0x10, null),
    STORE ("store", null, 0x36373839),
    STORE8 ("store8", null, 0x3a3c0000),
    STORE16 ("store16", null, 0x3b3d0000),
    STORE32 ("store32", null, 0x3e0000),
    LOAD ("load", null, 0x28292a2b),
    LOAD8_U ("load8_u", null, 0x2d310000),
    LOAD16_U ("load16_u", null, 0x2f330000),
    LOAD32_U ("load32_u", null, 0x00350000),
    LOAD8_S ("load8_s", null, 0x2c300000),
    LOAD16_S ("load16_s", null, 0x2e320000),
    LOAD32_S ("load32_s", null, 0x00340000),
    CONST ("const", null, 0x41424344),
    LOOP ("loop", 0x03, null),
    BLOCK ("block", 0x02, null),
    BR_IF ("br_if", 0x0d, null),
    BR ("br", 0x0c, null),
    IF ("if", 0x04, null),
    THEN ("then", null, null),
    ELSE ("else", 0x05, null),
    SELECT ("select", 0x1b, null),
    RETURN ("return", 0x0f, null),
    DROP ("drop", 0x1a, null),

    SQRT ("sqrt", null, 0x0000919f),
    CEIL ("ceil", null, 0x00008d9b),
    FLOOR ("floor", null, 0x00008e9c),
    ABS ("abs", null, 0x00008b99),

    WRAP_I64 ("wrap_i64", null, 0xa7000000),
    EXTEND_I32_S ("extend_i32_s", null, 0x00ac0000),
    EXTEND_I32_U ("extend_i32_u", null, 0x00ad0000),
    TRUNC_F32_S ("trunc_f32_s", null, 0xa8ae0000),
    TRUNC_F32_U ("trunc_f32_u", null, 0xa9af0000),
    TRUNC_F64_S ("trunc_f64_s", null, 0xaab00000),
    TRUNC_F64_U ("trunc_f64_u", null, 0xabb10000),
    DEMOTE_F64 ("demote_f64", null, 0x0000b600),
    PROMOTE_F32 ("promote_f32", null, 0x000000bb),
    CONVERT_I32_S ("convert_i32_s", null, 0x0000b2b7),
    CONVERT_I64_S ("convert_i64_s", null, 0x0000b4b9),
    CONVERT_I32_U ("convert_i32_u", null, 0x0000b3b8),
    CONVERT_I64_U ("convert_i64_u", null, 0x0000b5ba),

    REINTERPRET_F32 ("reinterpret_f32", null, 0xbc000000),
    REINTERPRET_F64 ("reinterpret_f64", null, 0x00bd0000),
    REINTERPRET_I32 ("reinterpret_i32", null, 0x0000be00),
    REINTERPRET_I64 ("reinterpret_i64", null, 0x000000bf),

    MEMORY_FILL ("memory.fill", null, null),
    MEMORY_COPY ("memory.copy", null, null),
    MEMORY_GROW ("memory.grow", 0x40, null),
    MEMORY_SIZE ("memory.size", 0x3f, null),

    EXPORT ("export", null, null),
    IMPORT ("import", null, null),
    LOCAL ("local", null, null),
    GLOBAL ("global", null, null),
    FUNC ("func", null, null),
    MODULE ("module", null, null),
    RESULT ("result", null, null),
    PARAM ("param", null, null),
    MEMORY ("memory", null, null),
    DATA ("data", null, null),
    MUT ("mut", null, null),

    UNREACHABLE ("unreachable", 0x00, null),
    END ("end", 0x0b, null),

    SPECIAL("<special>", null, null); // fake

    private final String name;
    private final Integer opcode4;
    private final Integer opcode;

    InstructionName(String name, Integer opcode, Integer opcode4) {
        this.name = name;
        this.opcode = opcode;
        this.opcode4 = opcode4;
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

    @Override
    public byte opcode() {
        if (opcode == null)
            throw new RuntimeException("No single opcode for " + this);
        return (byte) (int) opcode;
    }

    byte opcode(NumType numType) {
        if (numType == NumType.F64) return (byte) (opcode4 & 0xFF);
        if (numType == NumType.F32) return (byte) ((opcode4 >>> 8) & 0xFF);
        if (numType == NumType.I64) return (byte) ((opcode4 >>> 16) & 0xFF);
        if (numType == NumType.I32) return (byte) ((opcode4 >>> 24) & 0xFF);
        throw new RuntimeException("Unknown numType = " + numType);
    }

}
