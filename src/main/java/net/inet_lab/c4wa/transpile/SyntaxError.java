package net.inet_lab.c4wa.transpile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxError extends RuntimeException {
    public final Position pos;
    public final String msg;
    public final boolean is_error;

    public static class Location {
        final public String fileName;
        final public int lineno;

        public Location(String fileName, int lineno) {
            this.fileName = fileName;
            this.lineno = lineno;
        }
    }

    public static class Position {
        final public int line_st, line_en, pos_st, pos_en, arg_no;
        public Position(int line_st, int line_en, int pos_st, int pos_en, int arg_no) {
            this.line_st = line_st;
            this.line_en = line_en;
            this.pos_st = pos_st;
            this.pos_en = pos_en;
            this.arg_no = arg_no;
        }

        public Position(int line_st, int line_en, int pos_st, int pos_en) {
            this(line_st, line_en, pos_st, pos_en, -1);
        }

        public Position(int line_st, int pos_st, int arg_no) {
            this(line_st, line_st, pos_st, pos_st, arg_no);
        }

        public Position(int line_st, int pos_st) {
            this(line_st, line_st, pos_st, pos_st, -1);
        }

        public Position() {
            this(-1, -1, -1);
        }
    }

    public interface WarningInterface {
        void report(SyntaxError warn);
    }

    public SyntaxError(String msg, boolean is_error) {
        this(new Position(), msg, is_error);
    }

    public SyntaxError(Position pos, String msg, boolean is_error) {
        this.pos = pos;
        this.msg = msg;
        this.is_error = is_error;
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
