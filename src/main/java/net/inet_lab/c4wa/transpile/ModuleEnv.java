package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import net.inet_lab.c4wa.wat.Module;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleEnv implements Partial, PostprocessContext {
    final List<FunctionEnv> functions;
    final Map<String, FunctionDecl> funcDecl;
    final Map<String, VariableDecl> varDecl;
    final Map<Integer, Integer> strings;
    final Map<String, Struct> structs;
    private final Map<String, Integer> libraryRequests;

    final List<Byte> data;

    final int STACK_SIZE;
    final String GLOBAL_IMPORT_NAME;
    final MemoryState memoryState;
    final String MEMORY_NAME;

    final boolean support_bulk_mem;
    final int alignment;

    final static String STACK_VAR_NAME = "@stack";
    // `A? B: C` translates to (if A (then B) (else C)) if other B or C has complexity greater or equal than this value;
    // otherwise, (select B C A) is used (which will evaluate all arguments regardless)
    final static int IF_THEN_ELSE_SHORT_CIRCUIT_THRESHOLD = 6;

    SyntaxError.WarningInterface warningHandler;
    int arg_no;

    public ModuleEnv(Properties prop, int alignment) {
        funcDecl = new HashMap<>();
        varDecl = new HashMap<>();
        functions = new ArrayList<>();
        strings = new HashMap<>();
        structs = new HashMap<>();

        GLOBAL_IMPORT_NAME = prop.getProperty("module.importName");
        String memoryStatus = prop.getProperty("module.memoryStatus");
        if (memoryStatus.startsWith("import:")) {
            MEMORY_NAME = memoryStatus.substring(7);
            memoryState = MemoryState.IMPORT;
        } else if (memoryStatus.startsWith("export:")) {
            MEMORY_NAME = memoryStatus.substring(7);
            memoryState = MemoryState.EXPORT;
        } else if (memoryStatus.equals("internal")) {
            MEMORY_NAME = "";
            memoryState = MemoryState.INTERNAL;
        } else if (memoryStatus.equals("none")) {
            MEMORY_NAME = null;
            memoryState = MemoryState.NONE;
        } else
            throw new RuntimeException("Invalid value of property 'module.memoryStatus'");

        STACK_SIZE = memoryState == MemoryState.NONE ? 0 : Integer.parseInt(prop.getProperty("module.stackSize"));

        this.support_bulk_mem = List.of("y", "yes", "t", "true").contains(prop.getProperty("wasm.bulk-memory").toLowerCase());
        assert alignment == 1 || alignment == 2 || alignment == 4 || alignment == 8;
        this.alignment = alignment;

        data = new ArrayList<>();
        this.libraryRequests = new HashMap<>();

        addDeclaration(new FunctionDecl("memset", CType.VOID,
                new CType[]{CType.VOID.make_pointer_to(), CType.CHAR, CType.INT}, false, FunctionDecl.SType.BUILTIN));
        addDeclaration(new FunctionDecl("memcpy", CType.VOID,
                new CType[]{CType.VOID.make_pointer_to(), CType.VOID.make_pointer_to(), CType.INT}, false, FunctionDecl.SType.BUILTIN));
        addDeclaration(new FunctionDecl("memgrow", CType.INT, new CType[]{CType.INT}, false, FunctionDecl.SType.BUILTIN));
        addDeclaration(new FunctionDecl("memsize", CType.INT, new CType[0], false, FunctionDecl.SType.BUILTIN));
    }

    String requestLibraryFunc(String fName) {
        if (!libraryRequests.containsKey(fName))
            libraryRequests.put(fName, 0);
        return fName;
    }

    void registerLibraryFunc(String fName) {
        if (!libraryRequests.containsKey(fName))
            return;
        libraryRequests.put(fName, 1);
    }

    public Collection<String> requiredLibs() {
        Set<String> res = new HashSet<>();

        for (String fName : libraryRequests.keySet())
            if (libraryRequests.get(fName) == 0) {
                boolean found = false;
                for (String libName : library.keySet()) {
                    if (library.get(libName).contains(fName)) {
                        res.add(libName);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new RuntimeException("No such library function '" + fName + "'");
            }
        return res;
    }

    public Collection<String> missingLibraryFuncs() {
        return libraryRequests.keySet().stream().filter(x -> libraryRequests.get(x) == 0).collect(Collectors.toUnmodifiableList());
    }

    public void setWarningHandler(int arg_no, SyntaxError.WarningInterface warningHandler) {
        this.arg_no = arg_no;
        this.warningHandler = warningHandler;
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

        while (!stack.isEmpty()) {
            String fname = stack.pop();
            if (res.contains(fname))
                continue;

            res.add(fname);
            FunctionEnv f = fmap.get(fname);
            if (f == null)
                continue;

            for (String x : f.calls)
                stack.push(x);
        }
        return res;
    }

    private void missingFunctions() {
        for (String fname : funcDecl.keySet()) {
            FunctionDecl decl = funcDecl.get(fname);

            if (decl.is_used && decl.storage == FunctionDecl.SType.EXTERNAL)
                if (warningHandler != null)
                    warningHandler.report(new SyntaxError(decl.where_used, "'extern' Function '" + fname + "' declared but not defined",
                            true));
        }
    }

    public Module wat() {
        missingFunctions();
        Set<String> includedF = dependencyList();

        if (includedF.isEmpty() && warningHandler != null)
            warningHandler.report(new SyntaxError("empty module, make sure you have at least one extern function", false));

        boolean need_stack = functions.stream().filter(f -> includedF.contains(f.name)).anyMatch(f -> f.uses_stack);
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

        Set<String> includedG = new HashSet<>();
        for (FunctionEnv f : functions)
            if (includedF.contains(f.name))
                includedG.addAll(f.globals);

        // now we include everything non-imported
        for (VariableDecl v : varDecl.values())
            if (!v.imported && (v.name.startsWith("@") || includedG.contains(v.name)))
                elements.add(v.wat());

        if (memoryState == MemoryState.EXPORT)
            elements.add(new Memory(MEMORY_NAME, 1));
        else if (memoryState == MemoryState.INTERNAL)
            elements.add(new Memory(1));

        if (!data.isEmpty()) {
            byte[] data_arr = new byte[data.size()];
            int idx = 0;
            for (byte b : data)
                data_arr[idx++] = b;
            elements.add(new Data(STACK_SIZE, data_arr, data.size()));
        }

        for (FunctionEnv f : functions)
            if (includedF.contains(f.name))
                elements.add(f.wat());

        return (Module) ((new Module(elements)).postprocessList(this));
    }

    static final Map<String, Set<String>> library = Map.ofEntries(
            Map.entry("sys_minmax", Set.of("__max_32s", "__min_32s", "__max_32u", "__min_32u", "__max_64s", "__min_64s", "__max_64u", "__min_64u")),
            Map.entry("sys_bulkmem", Set.of("memcpy", "memset"))
    );
}