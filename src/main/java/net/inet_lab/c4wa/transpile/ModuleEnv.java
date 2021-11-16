package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import net.inet_lab.c4wa.wat.Module;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ModuleEnv implements Partial {
    final List<FunctionEnv> functions;
    final Map<String, FunctionDecl> funcDecl;
    final Map<String, VariableDecl> varDecl;
    final Map<String,Integer> strings;
    final Map<String,Struct> structs;

    final byte[] data;
    int data_len;

    final int DATA_OFFSET;
    final int DATA_LENGTH;
    final String GLOBAL_IMPORT_NAME;
    final MemoryState memoryState;
    final String MEMORY_NAME;

    public ModuleEnv (Properties prop) {
        funcDecl = new HashMap<>();
        varDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();
        structs = new HashMap<>();

        DATA_OFFSET = Integer.parseInt(prop.getProperty("module.dataOffset"));
        DATA_LENGTH = Integer.parseInt(prop.getProperty("module.dataLength"));
        GLOBAL_IMPORT_NAME = prop.getProperty("module.importName");
        String memoryStatus = prop.getProperty("module.memoryStatus");
        if (memoryStatus.startsWith("import:")) {
            MEMORY_NAME = memoryStatus.substring(7);
            memoryState = MemoryState.IMPORT;
        }
        else if (memoryStatus.startsWith("export:")) {
            MEMORY_NAME = memoryStatus.substring(7);
            memoryState = MemoryState.EXPORT;
        }
        else if (memoryStatus.equals("internal")) {
            MEMORY_NAME = "";
            memoryState = MemoryState.INTERNAL;
        }
        else if (memoryStatus.equals("none")) {
            MEMORY_NAME = null;
            memoryState = MemoryState.NONE;
        }
        else
            throw new RuntimeException("Invalid value of property 'module.memoryStatus'");

        data = new byte[DATA_LENGTH];
        data_len = 0;

        addDeclaration(new FunctionDecl("memset", null,
                new CType[]{CType.CHAR.make_pointer_to(), CType.CHAR, CType.INT}, false, false));
    }

    public void addStruct(String name, Struct struct) {
        if (structs.containsKey(name))
            throw new RuntimeException("Struct '" + name + "' already defined");
        structs.put(name, struct);
    }

    public void addFunction(FunctionEnv f) {
        functions.add(f);
    }

    public void addDeclaration(FunctionDecl functionDecl) {
        String name = functionDecl.name;

        FunctionDecl decl = funcDecl.get(name);

        if (decl != null) {
            if (!decl.equals(functionDecl))
                throw new RuntimeException("Inconsistent declaration of function '" + name + "'; was " +
                        decl.signature() + ", now " + functionDecl.signature());
        }

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

    private enum MemoryState {
        EXPORT,
        IMPORT,
        INTERNAL,
        NONE
    };

    public Module wat () {
        List<Instruction> elements = new ArrayList<>();

        for (FunctionDecl f : funcDecl.values())
            if (f.imported)
                elements.add(new Import(GLOBAL_IMPORT_NAME, f.name, f.wat()));

        for (VariableDecl v : varDecl.values())
            if (v.imported)
                elements.add(v.wat(GLOBAL_IMPORT_NAME));

        if (memoryState == MemoryState.IMPORT)
            elements.add(new Memory(GLOBAL_IMPORT_NAME, MEMORY_NAME, 1));

        for (VariableDecl v : varDecl.values())
            if (!v.imported)
                elements.add(v.wat(GLOBAL_IMPORT_NAME));

        if (memoryState == MemoryState.EXPORT)
            elements.add(new Memory(MEMORY_NAME, 1));
        else if (memoryState == MemoryState.INTERNAL)
            elements.add(new Memory( 1));

        if (data_len > 0)
            elements.add(new Data(DATA_OFFSET, data, data_len));

        for (FunctionEnv f : functions)
            elements.add(f.wat());

        return new Module(elements);
    }

}
