package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import net.inet_lab.c4wa.wat.Module;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleEnv implements Partial {
    final List<FunctionEnv> functions;
    final Map<String, FunctionDecl> funcDecl;
    final Map<String, VariableDecl> varDecl;
    final Map<String,Integer> strings;

    final byte[] data;
    int data_len;

    final static int DATA_OFFSET = 1024;
    final static int DATA_LENGTH = 1024;
    final static String GLOBAL_IMPORT_NAME = "c4wa";

    public ModuleEnv () {
        funcDecl = new HashMap<>();
        varDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();

        data = new byte[DATA_LENGTH];
        data_len = 0;
    }

    public void addFunction(FunctionEnv f) {
        functions.add(f);
    }

    public void addDeclaration(FunctionDecl functionDecl) {
        String name = functionDecl.name;

        if (funcDecl.containsKey(name))
            throw new RuntimeException("Function '" + name + "' already declared or defined");

        funcDecl.put(name, functionDecl);
    }

    public void addDeclaration(VariableDecl variableDecl) {
        String name = variableDecl.name;

        if (varDecl.containsKey(name))
            throw new RuntimeException("Global variable '" + name + "' already declared");

        varDecl.put(name, variableDecl);
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

    public Module wat () {
        List<Instruction> elements = new ArrayList<>();

        for (FunctionDecl f : funcDecl.values())
            if (f.imported)
                elements.add(new Import("c4wa", f.name, f.wat()));

        for (VariableDecl v : varDecl.values())
            elements.add(v.wat());

        elements.add(new Memory("memory", 1));

        if (data_len > 0)
            elements.add(new Data(DATA_OFFSET, data, data_len));

        for (FunctionEnv f : functions)
            elements.add(f.wat());

        return new Module(elements);
    }

}
