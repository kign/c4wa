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
    final Set<String> libraryFuncs;

    final byte[] data;
    int data_len;

    final int STACK_SIZE;
    final int DATA_SIZE;
    final String GLOBAL_IMPORT_NAME;
    final MemoryState memoryState;
    final String MEMORY_NAME;

    final static String STACK_VAR_NAME = "@stack";

    public ModuleEnv (Properties prop) {
        funcDecl = new HashMap<>();
        varDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();
        structs = new HashMap<>();
        libraryFuncs = new HashSet<>();

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

        STACK_SIZE = memoryState == MemoryState.NONE? 0 : Integer.parseInt(prop.getProperty("module.stackSize"));
        DATA_SIZE  = memoryState == MemoryState.NONE? 0 : Integer.parseInt(prop.getProperty("module.dataSize"));

        data = new byte[DATA_SIZE];
        data_len = 0;

        addDeclaration(new FunctionDecl("memset", null,
                new CType[]{CType.CHAR.make_pointer_to(), CType.CHAR, CType.INT}, false, false));
        addDeclaration(new FunctionDecl("memcpy", null,
                new CType[]{CType.CHAR.make_pointer_to(), CType.CHAR.make_pointer_to(), CType.INT}, false, false));
        // Note that `memory.grow` actually return value (old memory size), but you are free to ignore it
        // without invoking `drop`.
        // I don't think this is a common design patter though, so it's easier all around to simply make
        // `memory.grow` void.
        addDeclaration(new FunctionDecl("memgrow", null, new CType[]{CType.INT}, false, false));
        addDeclaration(new FunctionDecl("memsize", CType.INT, new CType[0], false, false));
    }

    public String library(String name) {
        libraryFuncs.add(name);
        return name;
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
        int res = STACK_SIZE + data_len;

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
    }

    public Module wat () {
        boolean need_stack = functions.stream().anyMatch(f -> f.uses_stack);
        if (need_stack) {
            VariableDecl stackDecl = new VariableDecl(CType.INT, STACK_VAR_NAME);
            stackDecl.imported = false;
            stackDecl.exported = false;
            stackDecl.mutable = true;
            stackDecl.initialValue = new Const(0);
            addDeclaration(stackDecl);
        }

        List<Instruction> elements = new ArrayList<>();

        // Everything imported must go first
        for (FunctionDecl f : funcDecl.values())
            if (f.imported)
                elements.add(new Import(GLOBAL_IMPORT_NAME, f.name, f.wat()));

        for (VariableDecl v : varDecl.values())
            if (v.imported)
                elements.add(v.wat(GLOBAL_IMPORT_NAME));

        if (memoryState == MemoryState.IMPORT)
            elements.add(new Memory(GLOBAL_IMPORT_NAME, MEMORY_NAME, 1));

        // now we include everything non-imported
        for (VariableDecl v : varDecl.values())
            if (!v.imported)
                elements.add(v.wat());

        if (memoryState == MemoryState.EXPORT)
            elements.add(new Memory(MEMORY_NAME, 1));
        else if (memoryState == MemoryState.INTERNAL)
            elements.add(new Memory( 1));

        if (data_len > 0)
            elements.add(new Data(STACK_SIZE, data, data_len));

        for (FunctionEnv f : functions)
            elements.add(f.wat());

        return new Module(elements);
    }

}
