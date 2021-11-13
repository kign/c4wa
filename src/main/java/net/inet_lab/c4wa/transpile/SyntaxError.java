package net.inet_lab.c4wa.transpile;

public class SyntaxError extends RuntimeException {
    final public int line_st;
    final public int line_en;
    final public int pos_st;
    final public int pos_en;
    final public String msg;

    public SyntaxError(int line_st, int line_en, int pos_st, int pos_en, String msg) {
        this.line_st = line_st;
        this.line_en = line_en;
        this.pos_st = pos_st;
        this.pos_en = pos_en;
        this.msg = msg;
    }
}
