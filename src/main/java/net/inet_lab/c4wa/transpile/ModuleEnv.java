package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import net.inet_lab.c4wa.wat.Module;

import java.util.*;

public class ModuleEnv implements Partial, PostprocessContext {
    final List<FunctionEnv> functions;
    final Map<String, FunctionDecl> funcDecl;
    final Map<String, VariableDecl> varDecl;
    final Map<Integer,Integer> strings;
    final Map<String,Struct> structs;
    final Set<String> libraryFunctions;

    final List<Byte> data;

    final int STACK_SIZE;
    final String GLOBAL_IMPORT_NAME;
    final MemoryState memoryState;
    final String MEMORY_NAME;

    final static String STACK_VAR_NAME = "@stack";
    // `A? B: C` translates to (if A (then B) (else C)) if other B or C has complexity greater or equal than this value;
    // otherwise, (select B C A) is used (which will evaluate all arguments regardless)
    final static int IF_THEN_ELSE_SHORT_CIRCUIT_THRESHOLD = 6;

    SyntaxError.WarningInterface warningHandler;
    int arg_no;

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

        data = new ArrayList<>();

        addDeclaration(new FunctionDecl("memset", CType.VOID,
                new CType[]{CType.VOID.make_pointer_to(), CType.CHAR, CType.INT}, false, FunctionDecl.SType.BUILTIN));
        addDeclaration(new FunctionDecl("memcpy", CType.VOID,
                new CType[]{CType.VOID.make_pointer_to(), CType.VOID.make_pointer_to(), CType.INT}, false, FunctionDecl.SType.BUILTIN));
        // Note that `memory.grow` actually return value (old memory size), but you are free to ignore it
        // without invoking `drop`.
        // I don't think this is a common design patter though, so it's easier all around to simply make
        // `memory.grow` void.
        addDeclaration(new FunctionDecl("memgrow", CType.VOID, new CType[]{CType.INT}, false, FunctionDecl.SType.BUILTIN));
        addDeclaration(new FunctionDecl("memsize", CType.INT, new CType[0], false, FunctionDecl.SType.BUILTIN));
    }

    public void setWarningHandler(int arg_no, SyntaxError.WarningInterface warningHandler) {
        this.arg_no = arg_no;
        this.warningHandler = warningHandler;
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

    public String addDeclaration(FunctionDecl functionDecl) {
        String name = functionDecl.name;

        FunctionDecl decl = funcDecl.get(name);

        if (decl != null) {
            if (!decl.sameSignature(functionDecl))
                return "Inconsistent declaration of function '" + name + "'; was " +
                        decl.signature() + ", now " + functionDecl.signature();

            int r = decl.legalInAnotherFile(functionDecl);

            if (r == 0 && !decl.canBeReplacedWith(functionDecl))
                return "Function '" + name + "' already defined or declared (" + decl.storage + " => " + functionDecl.storage + ")";

            if (r == -1)
                return null;
        }

        funcDecl.put(name, functionDecl);
        return null;
    }

    public void addDeclaration(VariableDecl variableDecl) {
        String name = variableDecl.name;

        if (varDecl.containsKey(name))
            throw new RuntimeException("Global variable '" + name + "' already declared");

        varDecl.put(name, variableDecl);
    }

    public int addString(List<Byte> bytes) {
        int hash = bytes.hashCode();

        Integer current_id = strings.get(hash);
        if (current_id != null)
            return current_id;

        int res = _addString(bytes);

        strings.put(hash, res);

        return res;
    }

    private int _addString(List<Byte> bytes) {
        int res = STACK_SIZE + data.size();

        data.addAll(bytes);
        data.add((byte) 0);

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

    private void missingFunctions() {
        for (String fname: funcDecl.keySet()) {
            FunctionDecl decl = funcDecl.get(fname);

            if (decl.is_used && decl.storage == FunctionDecl.SType.EXTERNAL)
                if (warningHandler != null)
                    warningHandler.report(new SyntaxError(decl.where_used, "'extern' Function '" + fname + "' declared but not defined",
                        true));
        }
    }

    public Module wat () {
        missingFunctions ();
        Set<String> included = dependencyList();

        if (included.isEmpty() && warningHandler != null)
            warningHandler.report(new SyntaxError("empty module, make sure you have at least one extern function", false));

        boolean need_stack = functions.stream().filter(f -> included.contains(f.name)).anyMatch(f -> f.uses_stack);
        if (need_stack) {
            VariableDecl stackDecl = new VariableDecl(CType.INT, STACK_VAR_NAME, true, new SyntaxError.Position());
            stackDecl.imported = false;
            stackDecl.exported = false;
            stackDecl.initialValue = new Const(1); // this is to avoid possible of zero value of &var
            addDeclaration(stackDecl);
        }

        List<Instruction> elements = new ArrayList<>();

        // Everything imported must go first
        for (FunctionDecl f : funcDecl.values())
            if (f.storage == FunctionDecl.SType.IMPORTED)
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

        if (!data.isEmpty()) {
            byte[] data_arr = new byte[data.size()];
            int idx = 0;
            for (byte b: data)
                data_arr[idx ++] = b;
            elements.add(new Data(STACK_SIZE, data_arr, data.size()));
        }

        for (FunctionEnv f : functions)
            if(included.contains(f.name))
                elements.add(f.wat());

        for (String libf: libraryFunctions) {
            Func code = library.get(libf);
            if (code == null)
                System.err.println("Library function '" + libf + "' isn't available");
            else
                elements.add(code);
        }

        return (Module)((new Module(elements)).postprocessList(this));
    }

    static final Map<String, Func> library = Map.ofEntries(
                Map.entry("@max_32s", new Func(List.of(
                        new Special("@max_32s"),
                        new Param("a", NumType.I32),
                        new Param("b", NumType.I32),
                        new Result(NumType.I32)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I32, false, false, true, new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")),
                                                new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")))))),
                Map.entry("@min_32s", new Func(List.of(
                        new Special("@min_32s"),
                        new Param("a", NumType.I32),
                        new Param("b", NumType.I32),
                        new Result(NumType.I32)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I32, false, false, true, new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")),
                                                new GetLocal(NumType.I32, "b"), new GetLocal(NumType.I32, "a")))))),
                Map.entry("@max_32u", new Func(List.of(
                        new Special("@max_32u"),
                        new Param("a", NumType.I32),
                        new Param("b", NumType.I32),
                        new Result(NumType.I32)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I32, false, false, false, new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")),
                                                new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")))))),
                Map.entry("@min_32u", new Func(List.of(
                        new Special("@min_32u"),
                        new Param("a", NumType.I32),
                        new Param("b", NumType.I32),
                        new Result(NumType.I32)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I32, false, false, false, new GetLocal(NumType.I32, "a"), new GetLocal(NumType.I32, "b")),
                                                new GetLocal(NumType.I32, "b"), new GetLocal(NumType.I32, "a")))))),
                Map.entry("@max_64s", new Func(List.of(
                        new Special("@max_64s"),
                        new Param("a", NumType.I64),
                        new Param("b", NumType.I64),
                        new Result(NumType.I64)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I64, false, false, true, new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")),
                                                new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")))))),
                Map.entry("@min_64s", new Func(List.of(
                        new Special("@min_64s"),
                        new Param("a", NumType.I64),
                        new Param("b", NumType.I64),
                        new Result(NumType.I64)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I64, false, false, true, new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")),
                                                new GetLocal(NumType.I64, "b"), new GetLocal(NumType.I64, "a")))))),
                Map.entry("@max_64u", new Func(List.of(
                        new Special("@max_64u"),
                        new Param("a", NumType.I64),
                        new Param("b", NumType.I64),
                        new Result(NumType.I64)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I64, false, false, false, new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")),
                                                new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")))))),
                Map.entry("@min_64u", new Func(List.of(
                        new Special("@min_64u"),
                        new Param("a", NumType.I64),
                        new Param("b", NumType.I64),
                        new Result(NumType.I64)),List.of(
                                new WrapExp(
                                        new Select(
                                                new Cmp(NumType.I64, false, false, false, new GetLocal(NumType.I64, "a"), new GetLocal(NumType.I64, "b")),
                                                new GetLocal(NumType.I64, "b"), new GetLocal(NumType.I64, "a")))))));
}
