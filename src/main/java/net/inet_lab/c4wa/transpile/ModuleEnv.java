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
    final Set<String> libraryFunctions;

    final byte[] data;
    int data_len;

    final int STACK_SIZE;
    final int DATA_SIZE;
    final String GLOBAL_IMPORT_NAME;
    final MemoryState memoryState;
    final String MEMORY_NAME;

    final static String STACK_VAR_NAME = "@stack";
    // `A? B: C` translates to (if A (then B) (else C)) if other B or C has complexity greater or equal than this value;
    // otherwise, (select B C A) is used (which will evaluate all arguments regardless)
    final static int IF_THEN_ELSE_SHORT_CIRCUIT_THRESHOLD = 6;

    public ModuleEnv (Properties prop) {
        funcDecl = new HashMap<>();
        varDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();
        structs = new HashMap<>();
        libraryFunctions = new HashSet<>();

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
        libraryFunctions.add(name);
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

    private Set<String> dependencyList() {
        Map<String, FunctionEnv> fmap = new HashMap<>();
        for (FunctionEnv f : functions)
                fmap.put(f.name, f);

        Deque<String> stack = new ArrayDeque<>();
        for (FunctionEnv f : functions)
            if (f.is_exported)
                stack.push(f.name);

        Set<String> res = new HashSet<>();

        while(!stack.isEmpty()) {
            String fname = stack.pop();
            if (res.contains(fname))
                continue;

            res.add(fname);
            FunctionEnv f = fmap.get(fname);
            if (f == null)
                continue;

            for (String x: f.calls)
                stack.push(x);
        }
        return res;
    }

    public Module wat () {
        Set<String> included = dependencyList();

        if (included.isEmpty())
            System.err.println("WARNING: empty module, make sure you have at least one extern function");

        boolean need_stack = functions.stream().filter(f -> included.contains(f.name)).anyMatch(f -> f.uses_stack);
        if (need_stack) {
            VariableDecl stackDecl = new VariableDecl(CType.INT, STACK_VAR_NAME, true);
            stackDecl.imported = false;
            stackDecl.exported = false;
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
            if(included.contains(f.name))
                elements.add(f.wat());

        for (String libf: libraryFunctions) {
            String code = library.get(libf);
            if (code == null)
                System.err.println("Library function '" + libf + "' isn't available");
            else
                elements.add(new FuncWat(libf, code));
        }

        return new Module(elements);
    }

    static final Map<String, String> library = Map.ofEntries(
            Map.entry("@max_32s", "(param $a i32) (param $b i32) (result i32) (select (get_local $a) (get_local $b) (i32.gt_s (get_local $a) (get_local $b)))"),
            Map.entry("@min_32s", "(param $a i32) (param $b i32) (result i32) (select (get_local $b) (get_local $a) (i32.gt_s (get_local $a) (get_local $b)))"),
            Map.entry("@max_32u", "(param $a i32) (param $b i32) (result i32) (select (get_local $a) (get_local $b) (i32.gt_u (get_local $a) (get_local $b)))"),
            Map.entry("@min_32u", "(param $a i32) (param $b i32) (result i32) (select (get_local $b) (get_local $a) (i32.gt_u (get_local $a) (get_local $b)))"),
            Map.entry("@max_64s", "(param $a i64) (param $b i64) (result i64) (select (get_local $a) (get_local $b) (i64.gt_s (get_local $a) (get_local $b)))"),
            Map.entry("@min_64s", "(param $a i64) (param $b i64) (result i64) (select (get_local $b) (get_local $a) (i64.gt_s (get_local $a) (get_local $b)))"),
            Map.entry("@max_64u", "(param $a i64) (param $b i64) (result i64) (select (get_local $a) (get_local $b) (i64.gt_u (get_local $a) (get_local $b)))"),
            Map.entry("@min_64u", "(param $a i64) (param $b i64) (result i64) (select (get_local $b) (get_local $a) (i64.gt_u (get_local $a) (get_local $b)))")
    );

}
