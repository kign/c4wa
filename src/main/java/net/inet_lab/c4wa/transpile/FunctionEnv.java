package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.wat.*;

import java.util.*;

public class FunctionEnv implements Partial {
    final String name;
    final CType returnType;
    final List<String> params;
    final List<String> locals;
    final boolean export;
    final Map<String, CType> varType;
    final Map<NumType, String> tempVars;
    final Deque<Block> blocks;

    Instruction[] instructions;
    int mem_offset;

    public FunctionEnv (String name, CType returnType, boolean export) {
        this.name = name;
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.locals = new ArrayList<>();
        this.export = export;
        this.mem_offset = 0;
        varType = new HashMap<>();
        blocks = new ArrayDeque<>();
        tempVars = new HashMap<>();

        blocks.push(new Block());
    }

    public void setMemOffset(int offset) {
        if (offset > mem_offset)
            mem_offset = offset;
    }

    public int getMemOffset() {
        return mem_offset;
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

    public void addInstructions(Instruction[] instructions) {
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

    public Func wat() {
        List<Instruction> attributes = new ArrayList<>();
        List<Instruction> elements = new ArrayList<>();

        attributes.add(new Special(name));

        if (export)
            attributes.add(new Export(name));

        for (String p : params)
            attributes.add(new Param(p, varType.get(p).asNumType()));

        if (returnType != null)
            attributes.add(new Result(returnType.asNumType()));

        for (String v : locals)
            elements.add(new Local(v, varType.get(v).asNumType()));

        for (NumType numType : tempVars.keySet())
            elements.add(new Local(tempVars.get(numType), numType));

        elements.addAll(Arrays.asList(instructions));
        return new Func(attributes, elements);
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
