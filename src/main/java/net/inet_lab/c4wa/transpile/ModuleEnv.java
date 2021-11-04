package net.inet_lab.c4wa.transpile;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleEnv implements Partial {
    final List<FunctionEnv> functions;
    final Map<String, FunctionDecl> funcDecl;
    final Map<String,Integer> strings;
    // final StringBuilder data;
    final byte[] data;
    int data_len;

    final static int DATA_OFFSET = 1024;
    final static int DATA_LENGTH = 1024;

    public ModuleEnv () {
        funcDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();
        //data = new StringBuilder();
        data = new byte[DATA_LENGTH];
        data_len = 0;
    }

    public void addFunction(FunctionEnv f) {
        functions.add(f);
    }

    public void addDeclaration(FunctionDecl fdecl) {
        String name = fdecl.name;

        if (funcDecl.containsKey(name))
            throw new RuntimeException("Function '" + name + "' already declared or defined");

        funcDecl.put(name, fdecl);
    }

    public int addString(String str) {
        if (strings.containsKey(str))
            return strings.get(str);

        int res = _addString(str);

        strings.put(str, res);

        return res;
    }

    private int _addString(String str) {
        int res = DATA_OFFSET + data_len;

        for(byte b: str.getBytes(StandardCharsets.UTF_8))
            data[data_len ++] = b;
        data[data_len++] = '\0';

        return res;
    }

    public void generateWat(final PrintStream out) throws IOException {
        out.println("(module ");

        for (FunctionDecl f : funcDecl.values())
            if (f.imported)
                out.println("(import \"c4wa\" \"" + f.name + "\" " + f.wat() + ")");

        out.println("(memory (export \"memory\") 1)");

        if (data_len > 0) {
            out.print("(data (i32.const " + DATA_OFFSET + ") \"");
            for (int i = 0; i < data_len; i ++) {
                byte b = data[i];
                if (0x20 <= b && b <= 0x7e && b != '\\')
                    out.print((char)b);
                else
                    out.printf("\\%02X", b);
            }
            out.println("\")");
        }

        for (FunctionEnv f: functions)
            f.generateWat(out);

        out.println(")");
    }
}
