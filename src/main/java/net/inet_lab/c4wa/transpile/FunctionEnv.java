package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FunctionEnv implements Partial, PostprocessContext {
    final String name;
    final @NotNull CType returnType;
    final private List<String> params;
    final boolean is_exported;
    final boolean vararg;
    final Map<NumType, String> tempVars;
    final Deque<Integer> blocks;
    final static String STACK_ENTRY_VAR = "@stack_entry";
    final ModuleEnv moduleEnv;
    final Set<String> calls;
    final Set<String> globals;

    Instruction[] instructions;
    boolean uses_stack;

    private Instruction watCode;
    final private List<Variable> variables;
    private boolean is_closed;

    public FunctionEnv (String name, CType returnType, ModuleEnv moduleEnv, boolean vararg, boolean export) {
        this.name = name;
        this.moduleEnv = moduleEnv;
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.vararg = vararg;
        this.is_exported = export;
        variables = new ArrayList<>();
        blocks = new ArrayDeque<>();
        tempVars = new HashMap<>();
        calls = new HashSet<>();
        globals = new HashSet<>();

        blocks.push(0);
        uses_stack = false;
        is_closed = false;
    }

    public void markAsUsingStack () {
        uses_stack = true;
    }

    public String registerVar(String name, String block_id, CType type,
                              boolean is_param, boolean is_mutable, boolean is_hidden,
                              SyntaxError.Position where_defined) {
        if (variables.stream().anyMatch(x -> Objects.equals(x.block_id, block_id) && x.name.equals(name)))
            return null;

        Variable v = new Variable(name, block_id, new VariableDecl(type, name, is_mutable, where_defined), is_param, is_hidden);
        variables.add(v);
        if (is_param)
            params.add(v.var_id);

        return v.var_id;
    }

    public String getVariableWAT(String variableId) {
        if (!is_closed)
            throw new RuntimeException("Function hasn't been closed yet");

        var res = variables.stream().filter(x -> x.var_id.equals(variableId)).findFirst();
        return res.map(variable -> variable.watName).orElse(variableId);
    }

    public boolean isWATNameReused(String watName) {
        if (!is_closed)
            throw new RuntimeException("Function hasn't been closed yet");

        return variables.stream().filter(x -> x.watName.equals(watName)).count() > 1;
    }

    public VariableDecl getVariableDecl(String variableId) {
        var res = variables.stream().filter(x -> x.var_id.equals(variableId)).findFirst();
        return res.map(variable -> variable.decl).orElse(null);
    }

    private boolean isParamHidden(String variableId) {
        var res = variables.stream().filter(x -> x.var_id.equals(variableId)).findFirst();
        return res.map(variable -> variable.is_hidden).orElse(false);
    }

    public String temporaryVar(NumType numType) {
        return tempVars.computeIfAbsent(numType, t -> "@temp_" + t);
    }

    public FunctionDecl makeDeclaration() {
        return new FunctionDecl(name, returnType,
                params.stream()
                        .filter(p -> !isParamHidden(p))
                        .map(p -> getVariableDecl(p).type)
                        .toArray(CType[]::new), vararg,
                is_exported? FunctionDecl.SType.EXPORTED : FunctionDecl.SType.INTERNAL);
    }

    public void setCode(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public String pushBlock() {
        int last = blocks.removeLast();
        blocks.addLast(last + 1);

        StringBuilder b = new StringBuilder();
        b.append("@block");
        for (int index : blocks)
            b.append('_').append(index);

        blocks.addLast(0);
        return b.toString();
    }

    public void popBlock () {
        blocks.removeLast ();
    }

    private List<LocalVar> assignWATNames() {
        int N = variables.size();
        List<LocalVar> localVars = new ArrayList<>();
        for (int i = 0; i < N; i ++) {
            Variable vi = variables.get(i);
            for (LocalVar local: localVars)
                if (local.numType == vi.getNumType()) {
                    boolean ok = true;
                    for (int j = 0; j < i && ok; j ++) {
                        Variable vj = variables.get(j);
                        if (vj.watName.equals(local.watName))
                            ok = vj.block_id != null && (vi.block_id == null || !vi.block_id.startsWith(vj.block_id));
                    }
                    if (ok) {
                        vi.watName = local.watName;
                        break;
                    }
                }
            if (vi.watName == null) {
                vi.watName = localVars.stream().anyMatch(x -> x.watName.equals(vi.name))? vi.var_id : vi.name;
                assert localVars.stream().noneMatch(x -> x.watName.equals(vi.watName));
                localVars.add(new LocalVar(vi.watName, vi.getNumType(), vi.is_param));
            }
        }

        if (name.equals("some_function_for_testing...")) {
            System.out.println("==> Function " + name);
            for (Variable v : variables)
                System.out.printf("%-15s%-20s%s\n", v.name, v.block_id == null?"<top level>":v.block_id, v.watName);
        }
        return localVars;
    }

    public void close() {
        if (blocks.size() != 1)
            throw new RuntimeException("Function " + name + " cannot be closed: blocks.size() = " + blocks.size());

        is_closed = true;
        warnUnusedVariables ();
        postprocessWat (assignWATNames());
    }

    public Instruction wat() {
        return watCode;
    }

    private void warnUnusedVariables() {
        for(Variable v: variables) {
            if (!v.decl.is_used && moduleEnv.warningHandler != null)
                moduleEnv.warningHandler.report(new SyntaxError(v.decl.where_defined,
                        "WARNING: Variable '" + v.name + "' in function '" + name + "' is not used"));
        }
    }

    private void postprocessWat(List<LocalVar> localVars) {
        List<Instruction> attributes = new ArrayList<>();
        List<Instruction> elements = new ArrayList<>();

        attributes.add(new Special(name));

        if (is_exported)
            attributes.add(new Export(name));

        for (LocalVar localVar : localVars)
            if (localVar.is_param)
                attributes.add(new Param(localVar.watName, localVar.numType));

        if (!returnType.is_void())
            attributes.add(new Result(returnType.asNumType()));

        if (uses_stack)
            elements.add(new Local(STACK_ENTRY_VAR, NumType.I32));

        for (LocalVar localVar : localVars)
            if (!localVar.is_param)
                elements.add(new Local(localVar.watName, localVar.numType));

        for (NumType numType : tempVars.keySet())
            elements.add(new Local(tempVars.get(numType), numType));

        if (uses_stack)
            elements.add(new SetLocal(STACK_ENTRY_VAR, new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME)));

        elements.addAll(Arrays.asList(instructions));

        if (!(elements.size() > 0 && elements.get(elements.size() - 1) instanceof ParseTreeVisitor.DelayedReturn)) {
            if (returnType.is_void()) {
                if (uses_stack)
                    elements.add(new SetGlobal(ModuleEnv.STACK_VAR_NAME, new GetLocal(NumType.I32, FunctionEnv.STACK_ENTRY_VAR)));
            }
            else
                elements.add(new Unreachable());
        }

        Func _watCode = new Func(attributes, elements);
        watCode = _watCode.postprocessList(this).postprocessList(this).postprocessList(this);
    }

    static private class LocalVar {
        final String watName;
        final NumType numType;
        final boolean is_param;

        LocalVar(String watName, NumType numType, boolean is_param) {
            this.watName = watName;
            this.numType = numType;
            this.is_param = is_param;
        }
    }

    static private class Variable {
        final String name;
        final String block_id;
        final String var_id;
        final VariableDecl decl;
        final boolean is_param;
        final boolean is_hidden;

        String watName;

        Variable(String name, String block_id, VariableDecl decl, boolean is_param, boolean is_hidden) {
            this.name = name;
            this.block_id = block_id;
            this.decl = decl;
            this.is_param = is_param;
            this.is_hidden = is_hidden;

            this.var_id = name + (block_id == null? "" : block_id);
            watName = null;
        }

        NumType getNumType() {
            return decl.inStack ? NumType.I32 : decl.type.asNumType();
        }

    }
}
