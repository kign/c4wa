package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;

import java.util.*;

public class FunctionEnv implements Partial, PostprocessContext {
    final String name;
    final CType returnType;
    final List<String> params;
    final List<String> locals;
    final boolean export;
    final Map<String, CType> varType;
    final Map<NumType, String> tempVars;
    final Deque<Block> blocks;
    final static String STACK_ENTRY_VAR = "@stack_entry";

    Instruction[] instructions;
    boolean uses_stack;

    public FunctionEnv (String name, CType returnType, boolean export) {
        this.name = name;
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.locals = new ArrayList<>();
        this.export = export;
        varType = new HashMap<>();
        blocks = new ArrayDeque<>();
        tempVars = new HashMap<>();

        blocks.push(new Block());
        uses_stack = false;
    }

    public void markAsUsingStack () {
        uses_stack = true;
    }

    public void registerVar(String name, CType type, boolean is_param) {
        if (varType.containsKey(name))
            throw new RuntimeException("Variable " + name + " already defined");
        varType.put(name, type);
        if (is_param)
            params.add(name);
        else
            locals.add(name);
    }

    public String temporaryVar(NumType numType) {
        return tempVars.computeIfAbsent(numType, t -> "@temp_" + t);
    }

    public FunctionDecl makeDeclaration() {
        return new FunctionDecl(name, returnType,
                params.stream().map(varType::get).toArray(CType[]::new), false, false);
    }

    public void setCode(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public String pushBlock(Instruction postfix) {
        Block last = blocks.removeLast();
        blocks.addLast(new Block(postfix, last.index + 1));
        blocks.addLast(new Block());
        return getBlockId();
    }

    public String getBlockId () {
        StringBuilder b = new StringBuilder();

        b.append("@block");
        int idx = 1;
        for (Block block : blocks) {
            b.append('_').append(block.index);
            if (++ idx >= blocks.size())
                break;
        }

        return b.toString();
    }

    public Instruction getBlockPostfix () {
        int idx = 1;
        for (Block block : blocks) {
            if (++ idx >= blocks.size())
                return block.postfix;
        }
        return null;
    }

    public void popBlock () {
        blocks.removeLast ();
    }

    public void close() {
        String msg = "Function " + name + " cannot be closed: ";
        if (blocks.size() != 1)
            throw new RuntimeException(msg + "blocks.size() = " + blocks.size());

    }

    public Instruction wat() {
        List<Instruction> attributes = new ArrayList<>();
        List<Instruction> elements = new ArrayList<>();

        attributes.add(new Special(name));

        if (export)
            attributes.add(new Export(name));

        for (String p : params)
            attributes.add(new Param(p, varType.get(p).asNumType()));

        if (returnType != null)
            attributes.add(new Result(returnType.asNumType()));

        if (uses_stack)
            elements.add(new Local(STACK_ENTRY_VAR, NumType.I32));

        for (String v : locals)
            elements.add(new Local(v, varType.get(v).asNumType()));

        for (NumType numType : tempVars.keySet())
            elements.add(new Local(tempVars.get(numType), numType));

        if (uses_stack)
            elements.add(new SetLocal(STACK_ENTRY_VAR, new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME)));

        elements.addAll(Arrays.asList(instructions));

        if (uses_stack && !(elements.get(elements.size() - 1) instanceof ParseTreeVisitor.PreparedReturn))
            elements.add(new SetGlobal(ModuleEnv.STACK_VAR_NAME, new GetLocal(NumType.I32, FunctionEnv.STACK_ENTRY_VAR)));

        Instruction watCode = new Func(attributes, elements);
        Instruction[] res = watCode.postprocess(this);
        if (res.length != 1)
            throw new RuntimeException("Function postprocessing returned " + res.length + " instructions");
        return res[0];
    }

    static class Block {
        final Instruction postfix;
        final int index;

        Block(Instruction postfix, int index) {
            this.postfix = postfix;
            this.index = index;
        }

        Block() {
            this.postfix = null;
            this.index = 0;
        }
    }
}
