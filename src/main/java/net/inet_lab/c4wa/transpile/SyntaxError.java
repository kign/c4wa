package net.inet_lab.c4wa.transpile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxError extends RuntimeException {
    final public int line_st;
    final public int line_en;
    final public int pos_st;
    final public int pos_en;
    final public String msg;

    public SyntaxError(String msg) {
        this.msg = msg;
        this.line_st = this.line_en = this.pos_st = this.pos_en = -1;
    }

    public SyntaxError(int line_st, int line_en, int pos_st, int pos_en, String msg) {
        this.line_st = line_st;
        this.line_en = line_en;
        this.pos_st = pos_st;
        this.pos_en = pos_en;
        this.msg = msg;
    }

    public String fileName;
    public int lineno;
    public void locate(List<String> lines) {
        Pattern lineDirective = Pattern.compile("^#\\s+(\\d+)\\s+\"(.+)\"", Pattern.CASE_INSENSITIVE);

        for (int i = line_st - 2; i >= 0; i --) {
            Matcher m = lineDirective.matcher(lines.get(i));
            if (m.find()) {
                fileName = m.group(2);
                lineno = Integer.parseInt(m.group(1)) + (line_st - i - 2);
                return;
            }
        }

        fileName = "<unknown>";
        lineno = line_st;
    }
}
