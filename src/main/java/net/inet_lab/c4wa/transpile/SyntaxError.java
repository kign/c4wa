package net.inet_lab.c4wa.transpile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxError extends RuntimeException {
    public final Position pos;
    final public String msg;

    public static class Location {
        final public String fileName;
        final public int lineno;

        public Location(String fileName, int lineno) {
            this.fileName = fileName;
            this.lineno = lineno;
        }
    }

    public static class Position {
        final public int line_st, line_en, pos_st, pos_en;
        public Position(int line_st, int line_en, int pos_st, int pos_en) {
            this.line_st = line_st;
            this.line_en = line_en;
            this.pos_st = pos_st;
            this.pos_en = pos_en;
        }

        public Position(int line_st, int pos_st) {
            this.line_st = line_st;
            this.line_en = line_st;
            this.pos_st = pos_st;
            this.pos_en = pos_st;
        }

        public Position() {
            this.line_st = this.line_en = this.pos_st = this.pos_en = -1;
        }
    }

    public interface WarningInterface {
        void report(SyntaxError warn);
    }

    public SyntaxError(String msg) {
        this.msg = msg;
        this.pos = new Position();
    }

    public SyntaxError(Position pos, String msg) {
        this.pos = pos;
        this.msg = msg;
    }

    public Location locate(List<String> lines) {
        Pattern lineDirective = Pattern.compile("^#\\s+(\\d+)\\s+\"(.+)\"", Pattern.CASE_INSENSITIVE);

        for (int i = pos.line_st - 2; i >= 0; i --) {
            Matcher m = lineDirective.matcher(lines.get(i));
            if (m.find())
                return new Location(m.group(2), Integer.parseInt(m.group(1)) + (pos.line_st - i - 2));
        }

        return new Location("<unknown>", pos.line_st);
    }
}
