package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

public class ParseTreeVisitor extends c4waBaseVisitor<Partial> {
    private FunctionEnv functionEnv;
    private ModuleEnv moduleEnv;
    final private Deque<BlockEnv> blockStack;

    final private static String CONT_SUFFIX = "_continue";
    final private static String BREAK_SUFFIX = "_break";

    public ParseTreeVisitor() {
        blockStack = new ArrayDeque<>();
    }

    static class ParamList implements Partial {
        final VariableDecl[] paramList;
        final String struct_name;
        ParamList(VariableDecl[] paramList, String struct_name) {
            this.paramList = paramList;
            this.struct_name = struct_name;
        }
        ParamList(VariableDecl[] paramList) {
            this.paramList = paramList;
            this.struct_name = null;
        }
        ParamList() {
            this.paramList = new VariableDecl[0];
            this.struct_name = null;
        }
    }

    static class VariableWrapper implements Partial {
        final String name;
        final int ref_level;
        VariableWrapper(String name) {
            this.name = name;
            this.ref_level = 0;
        }
        VariableWrapper(VariableWrapper w) {
            this.name = w.name;
            this.ref_level = w.ref_level + 1;
        }
    }

    static class OneInstruction implements Partial {
        final Instruction instruction;
        final CType type;

        OneInstruction(Instruction instruction, CType type) {
            this.instruction = instruction;
            this.type = type;
        }
    }

    static class NoOp implements Partial {
    }

    static class InstructionList implements Partial {
        final OneInstruction[] instructions;
        boolean need_block;
        InstructionList() {
            this.instructions = new OneInstruction[0];
            need_block = false;
        }
        InstructionList(OneInstruction[] instructions) {
            this.instructions = instructions;
            need_block = false;
        }
        InstructionList(OneInstruction[] instructions, boolean need_block) {
            this.instructions = instructions;
            this.need_block = need_block;
        }
        Instruction[] extract() {
            return Arrays.stream(instructions).map(x -> x.instruction).toArray(Instruction[]::new);
        }
    }

    static class OneFunction implements Partial {
        final FunctionEnv func;
        final c4waParser.Composite_blockContext code;
        OneFunction(FunctionEnv func, c4waParser.Composite_blockContext code) {
            this.func = func;
            this.code = code;
        }
    }

    static class BlockEnv {
        int offset;
        final int start_offset;
        final List<Instruction> prefix;
        String block_id;
        Instruction block_postfix;
        boolean need_block;
        BlockEnv (int offset) {
            this.start_offset = offset;
            this.offset = offset;
            prefix = new ArrayList<>();
            block_id = null;
        }
        int getOffset() {
            int res = offset;
            offset += 8;
            return res;
        }
        void reset() {
            offset = this.start_offset;
            prefix.clear();
        }
        void markAsLoop(String block_id, Instruction block_postfix) {
            this.block_id = block_id;
            this.block_postfix = block_postfix;
            need_block = false;
        }
    }

    @Override
    public ModuleEnv visitModule(c4waParser.ModuleContext ctx) {
        moduleEnv = new ModuleEnv();

        List<OneFunction> functions = new ArrayList<>();

        for (var g : ctx.global_decl()) {
            Partial parseGlobalDecl = visit(g);

            if (parseGlobalDecl instanceof FunctionDecl)
                moduleEnv.addDeclaration((FunctionDecl) parseGlobalDecl);
            else if (parseGlobalDecl instanceof OneFunction) {
                moduleEnv.addDeclaration(((OneFunction) parseGlobalDecl).func.makeDeclaration());
                functions.add((OneFunction) parseGlobalDecl);
            }
            else if (parseGlobalDecl instanceof VariableDecl) {
                moduleEnv.addDeclaration((VariableDecl) parseGlobalDecl);
            }
            else if (parseGlobalDecl instanceof ParamList) {
                String name = ((ParamList) parseGlobalDecl).struct_name;
                moduleEnv.addStruct(name, new Struct(name, ((ParamList)parseGlobalDecl).paramList));
            }
            else
                throw fail(g, "global item", "Unknown class " + parseGlobalDecl.getClass());
        }

        int mem_offset = 0;
        for (var oneFunc : functions) {
            functionEnv = oneFunc.func;
            functionEnv.setMemOffset(mem_offset);

            functionEnv.addInstructions(Arrays.stream(((InstructionList) visit(oneFunc.code)).instructions).map(x -> x.instruction).toArray(Instruction[]::new));
            functionEnv.close();
            moduleEnv.addFunction(functionEnv);
            mem_offset = functionEnv.getMemOffset();
        }

        return moduleEnv;
    }

    @Override
    public ParamList visitStruct_definition(c4waParser.Struct_definitionContext ctx) {
        return new ParamList(ctx.variable_decl().stream().map(this::visit).toArray(VariableDecl[]::new),
                ctx.ID().getText());
    }

    @Override
    public VariableDecl visitGlobal_decl_variable(c4waParser.Global_decl_variableContext ctx) {
        VariableDecl decl = (VariableDecl) visit(ctx.variable_decl());
        decl.exported = ctx.EXTERN() != null;
        decl.imported = ctx.STATIC() == null;
        decl.mutable = ctx.CONST() == null;

        if (ctx.CONSTANT() == null) {
            if (!decl.imported)
                throw fail(ctx, "global variable", "non-imported variable must be initialized");
        }
        else {
            if (decl.mutable && decl.imported)
                throw fail(ctx, "global variable","Imported global variable cannot be initialized, declare 'const' or 'static'");
            decl.initialValue = parseConstant(ctx, decl.type, ctx.CONSTANT().getText());
            if (!decl.mutable)
                decl.imported = false;
        }

        return decl;
    }

    @Override
    public FunctionDecl visitGlobal_decl_function(c4waParser.Global_decl_functionContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());
        CType[] params = ctx.variable_type().stream().map(this::visit).toArray(CType[]::new);
        boolean anytype = params.length == 0;
        if (params.length == 1 && params[0] == null) // no_arg_func(void)
            params = new CType[0];

        return anytype
                ? new FunctionDecl(variableDecl.name, variableDecl.type, null, true, true)
                : new FunctionDecl(variableDecl.name, variableDecl.type, params, false, true)
                ;
    }

    @Override
    public OneFunction visitFunction_definition(c4waParser.Function_definitionContext ctx) {
        VariableDecl funcDecl = (VariableDecl) visit(ctx.variable_decl());
        ParamList paramList = (ctx.param_list() == null)?new ParamList(): (ParamList) visit(ctx.param_list());

        functionEnv = new FunctionEnv(funcDecl.name, funcDecl.type,ctx.EXTERN() != null);

        Arrays.stream(paramList.paramList).forEach(x -> functionEnv.registerVar(x.name, x.type, true));

        return new OneFunction(functionEnv, ctx.composite_block());
    }

    @Override
    public ParamList visitParam_list(c4waParser.Param_listContext ctx) {
        return new ParamList(ctx.variable_decl().stream().map(this::visit).toArray(VariableDecl[]::new));
    }

    @Override
    public VariableDecl visitVariable_decl(c4waParser.Variable_declContext ctx) {
        VariableWrapper variableWrapper = (VariableWrapper) visit(ctx.variable_with_modifiers());
        CType type = (CType) visit(ctx.primitive());

        for (int i = 0; i < variableWrapper.ref_level; i ++)
            type = type.make_pointer_to();
        return new VariableDecl(type, variableWrapper.name);
    }

    @Override
    public VariableWrapper visitVariable_with_modifiers_array(c4waParser.Variable_with_modifiers_arrayContext ctx) {
        return new VariableWrapper((VariableWrapper) visit(ctx.variable_with_modifiers()));
    }

    @Override
    public VariableWrapper visitVariable_with_modifiers_name(c4waParser.Variable_with_modifiers_nameContext ctx) {
        return new VariableWrapper(ctx.ID().getText());
    }

    @Override
    public VariableWrapper visitVariable_with_modifiers_pointer(c4waParser.Variable_with_modifiers_pointerContext ctx) {
        return new VariableWrapper((VariableWrapper) visit(ctx.variable_with_modifiers()));
    }

    @Override
    public CType visitVariable_type(c4waParser.Variable_typeContext ctx) {
        CType type = (CType) visit(ctx.primitive());

        for (int i = 0; i < ctx.MULT().size(); i++)
            type = type.make_pointer_to();

        return type;
    }

    @Override
    public InstructionList visitBlock(c4waParser.BlockContext ctx) {
        if (ctx.composite_block() != null)
            return (InstructionList) visit(ctx.composite_block());
        else {
            BlockEnv blockEnv = new BlockEnv(functionEnv.getMemOffset());
            var parent = ctx.getParent();
            if (parent instanceof c4waParser.Element_do_whileContext ||
                    parent instanceof c4waParser.Element_forContext)
                blockEnv.markAsLoop(functionEnv.getBlockId(), functionEnv.getBlockPostfix());

            List<OneInstruction> res = new ArrayList<>();

            blockStack.push(blockEnv);
            Partial parsedElem = visit(ctx.element());

            for (var i : blockEnv.prefix)
                res.add(new OneInstruction(i, null));
            if (parsedElem instanceof OneInstruction)
                res.add((OneInstruction) parsedElem);
            else if (parsedElem instanceof InstructionList)
                res.addAll(Arrays.asList(((InstructionList) parsedElem).instructions));
            blockStack.pop();

            return new InstructionList(res.toArray(OneInstruction[]::new), blockEnv.need_block);
        }
    }

    @Override
    public InstructionList visitComposite_block(c4waParser.Composite_blockContext ctx) {
        BlockEnv blockEnv = new BlockEnv(functionEnv.getMemOffset());

        // A hack obviously, but that's the best way I could find to meaningfully pass the knowledge
        // Basically we must know if this block is one associated with
        var parent = ctx.getParent();
        if (parent instanceof c4waParser.BlockContext)
            parent = parent.getParent();
        if (parent instanceof c4waParser.Element_do_whileContext ||
                parent instanceof c4waParser.Element_forContext)
            blockEnv.markAsLoop(functionEnv.getBlockId(), functionEnv.getBlockPostfix());

        blockStack.push(blockEnv);
        List<OneInstruction> res = new ArrayList<>();

        int idx = 0;
        for (var blockElem : ctx.element()) {
            idx ++;
            Partial parsedElem = visit(blockElem);
            if (parsedElem == null)
                throw fail(ctx, "block", "Instruction number " + idx + " was not parsed" +
                        ((idx == 1)?"":" (last parsed was '" + ctx.element(idx - 2).getText() + "')"));
            for (var i : blockEnv.prefix)
                res.add(new OneInstruction(i, null));
            blockEnv.reset();
            if (parsedElem instanceof OneInstruction)
                res.add((OneInstruction) parsedElem);
            else if (parsedElem instanceof InstructionList)
                res.addAll(Arrays.asList(((InstructionList) parsedElem).instructions));
        }

        blockStack.pop();
        return new InstructionList(res.toArray(OneInstruction[]::new), blockEnv.need_block);
    }

    @Override
    public OneInstruction visitElement_do_while(c4waParser.Element_do_whileContext ctx) {
        OneInstruction condition = (OneInstruction) visit(ctx.expression());

        String block_id = functionEnv.pushBlock(null);
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        InstructionList body = (InstructionList) visit(ctx.block());
        functionEnv.popBlock();

        if (condition.type == null)
            throw fail(ctx, "do_while", "Expression '" + ctx.expression().getText() +
                    "' has no type (any type would have worked in WASM as 'boolean', but there is none)");

        List<Instruction> body_elems = new ArrayList<>();
        for(var i : body.instructions)
            body_elems.add(i.instruction);

        body_elems.add(new BrIf(block_id_cont, condition.instruction));
        if (body.need_block)
            return new OneInstruction(
                    new Block(block_id_break, new Instruction[]{
                            new Loop(block_id_cont, body_elems.toArray(Instruction[]::new))}),
                    null);
        else
            return new OneInstruction(new Loop(block_id_cont, body_elems.toArray(Instruction[]::new)), null);
    }

    @Override
    public OneInstruction visitElement_for(c4waParser.Element_forContext ctx) {
        OneInstruction prestat = (OneInstruction) visit(ctx.pre);

        OneInstruction condition = (OneInstruction) visit(ctx.expression());
        if (condition != null && condition.type == null)
            throw fail(ctx, "for", "Expression '" + ctx.expression().getText() +
                    "' has no type (any type would have worked in WASM as 'boolean', but there is none)");

        OneInstruction poststat = (OneInstruction) visit(ctx.post);

        String block_id = functionEnv.pushBlock(poststat == null?null:poststat.instruction);
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        InstructionList body = (InstructionList) visit(ctx.block());
        functionEnv.popBlock();

        List<Instruction> loop_elems = new ArrayList<>();
        if (condition != null)
            loop_elems.add(new BrIf(block_id_break, condition.instruction.Not(condition.type.asNumType())));
        for (var i : body.instructions)
            loop_elems.add(i.instruction);
        if (poststat != null)
            loop_elems.add(poststat.instruction);
        loop_elems.add(new Br(block_id_cont));

        List<Instruction> block_elems = new ArrayList<>();
        if (prestat != null)
            block_elems.add(prestat.instruction);
        block_elems.add(new Loop(block_id_cont, loop_elems.toArray(Instruction[]::new)));

        return new OneInstruction(
                new Block(block_id_break, block_elems.toArray(Instruction[]::new)),
                null);
    }

    @Override
    public Partial visitElement_break_continue_if(c4waParser.Element_break_continue_ifContext ctx) {
        boolean is_break = ctx.BREAK() != null;
        OneInstruction condition = (OneInstruction) visit(ctx.expression());

        if (condition.type == null)
            throw fail(ctx, "break if", "condition of type void");

        return break_if(ctx, is_break, condition);
    }

    @Override
    public Partial visitElement_break_continue(c4waParser.Element_break_continueContext ctx) {
        boolean is_break = ctx.BREAK() != null;

        return break_if(ctx, is_break, null);
    }

    private Partial break_if(ParserRuleContext ctx, boolean is_break, OneInstruction condition) {
        BlockEnv blockEnv = null;
        for (var b : blockStack)
            if (b.block_id != null) {
                blockEnv = b;
                break;
            }

        if (blockEnv == null)
            throw fail(ctx, is_break ? "break" : "continue", "cannot find eligible block");

        String ref = blockEnv.block_id + (is_break ? BREAK_SUFFIX : CONT_SUFFIX);
        if (is_break)
            blockEnv.need_block = true;

        if (blockEnv.block_postfix == null || is_break) {
            if (condition == null)
                return new OneInstruction(new Br(ref), null);
            else
                return new OneInstruction(new BrIf(ref, condition.instruction), null);
        }
        else {
            if (condition == null)
                return new InstructionList(new OneInstruction[]{new OneInstruction(blockEnv.block_postfix, null),
                new OneInstruction(new Br(ref), null)});
            else
                return new OneInstruction(
                        new IfThenElse(condition.instruction,
                        null,
                        new Instruction[]{blockEnv.block_postfix, new Br(ref)},
                        null), null);
        }
    }

    @Override
    public OneInstruction visitElement_if(c4waParser.Element_ifContext ctx) {
        OneInstruction condition = (OneInstruction) visit(ctx.expression());
        InstructionList thenList = (InstructionList) visit(ctx.block());

        return if_then_else(ctx, condition, thenList, null);
    }

    @Override
    public OneInstruction visitElement_if_else(c4waParser.Element_if_elseContext ctx) {
        OneInstruction condition = (OneInstruction) visit(ctx.expression());
        InstructionList thenList = (InstructionList) visit(ctx.block(0));
        InstructionList elseList = (InstructionList) visit(ctx.block(1));

        return if_then_else(ctx, condition, thenList, elseList);
    }

    @Override
    public OneInstruction visitExpression_if_else(c4waParser.Expression_if_elseContext ctx) {
        OneInstruction condition = (OneInstruction) visit(ctx.expression(0));
        OneInstruction thenExp = (OneInstruction) visit(ctx.expression(1));
        OneInstruction elseExp = (OneInstruction) visit(ctx.expression(2));

        if (condition.type == null)
            throw fail(ctx, "ternary", "condition type void isn't allowed");

        if (thenExp.type == null)
            throw fail(ctx, "ternary", "first argument type void isn't allowed");

        if (elseExp.type == null)
            throw fail(ctx, "ternary", "second argument type void isn't allowed");

        if (!thenExp.type.same(elseExp.type))
            throw fail(ctx, "ternary", "argument types '" + thenExp.type +
                    "' and '" + elseExp.type + "' are incompatible");

        return new OneInstruction(new IfThenElse(condition.instruction, thenExp.type.asNumType(), new Instruction[]{ thenExp.instruction },
                new Instruction[]{ elseExp.instruction }), thenExp.type);
    }

    private OneInstruction if_then_else(ParserRuleContext ctx, OneInstruction condition, InstructionList thenList,
                                        InstructionList elseList) {
        if (condition.type == null)
            throw fail(ctx, "if...then", "condition type void isn't allowed");

        CType resType = (thenList.instructions.length == 1 &&
                            elseList != null &&
                            elseList.instructions.length == 1 &&
                            thenList.instructions[0].type != null &&
                            elseList.instructions[0].type != null &&
                            thenList.instructions[0].type.same(elseList.instructions[0].type))
                ? thenList.instructions[0].type
                : null;

        return new OneInstruction(new IfThenElse(condition.instruction,
                (resType == null)?null:resType.asNumType(),
                thenList.extract(), (elseList == null)?null:elseList.extract()),
                resType);
    }

    @Override
    public NoOp visitElement_empty(c4waParser.Element_emptyContext ctx) {
        return new NoOp();
    }

    @Override
    public OneInstruction visitFunction_call(c4waParser.Function_callContext ctx) {
        String fname = ctx.ID().getText();
        FunctionDecl decl = moduleEnv.funcDecl.get(fname);

        if (decl == null)
            throw fail(ctx, "function call", "Function '" + fname + "' not defined or declared");

        InstructionList args = (ctx.arg_list() == null)?(new InstructionList()):(InstructionList) visit(ctx.arg_list());
        Instruction[] call_args;
        if (decl.anytype) {
            BlockEnv blockEnv = blockStack.peek();
            int idx = 0;
            assert blockEnv != null;
            int offset = blockEnv.offset;
            for (var arg : args.instructions) {
                idx ++;
                if (arg.type == null)
                    throw fail(ctx, "function call", "Argument number " + idx + " doesn't have type");

                Const constOffset = new Const(blockEnv.getOffset());
                if (arg.type.is_32()) {
                    NumType t64 = arg.type.is_int()? NumType.I64 : NumType.F64;
                    blockEnv.prefix.add(new Store(t64, constOffset,
                            new GenericCast(arg.type.asNumType(), t64, arg.type.is_signed(), arg.instruction)));
                }
                else
                    blockEnv.prefix.add(new Store(arg.type.asNumType(), constOffset, arg.instruction));
            }

            functionEnv.setMemOffset(blockEnv.offset);
            call_args = new Instruction[2];
            call_args[0] = new Const(offset);
            call_args[1] = new Const(args.instructions.length);
        }
        else {
            if (decl.params.length != args.instructions.length)
                throw fail(ctx, "function call", "Function '" + fname +
                        "' expects " + decl.params.length + " arguments, provided " + args.instructions.length);

            call_args = new Instruction[args.instructions.length];
            for(int idx = 0; idx < decl.params.length; idx ++) {
                if (args.instructions[idx].type == null)
                    throw fail(ctx, "function call", "Argument number " + (idx + 1) +
                            " of function '" + fname + "' received argument with no type");

                if (!decl.params[idx].isValidRHS(args.instructions[idx].type))
                    throw fail(ctx, "function call", "Argument number " + (idx + 1) +
                            " of function '" + fname + "' expects type '" + decl.params[idx] +
                            "', received type '" + args.instructions[idx].type + "'");

                call_args[idx] = args.instructions[idx].instruction;
            }
        }
        return new OneInstruction(new Call(fname, call_args), decl.returnType);
    }

    @Override
    public InstructionList visitArg_list(c4waParser.Arg_listContext ctx) {
        return new InstructionList(ctx.expression().stream().map(this::visit).toArray(OneInstruction[]::new));
    }

    @Override
    public OneInstruction visitReturn_expression(c4waParser.Return_expressionContext ctx) {
        OneInstruction expression = (OneInstruction) visit(ctx.expression());
        if (expression.type == null)
            throw fail(ctx, "return", "expression has no type");
        if (functionEnv.returnType == null)
            throw fail(ctx, "return", "Function '" + functionEnv.name + "' doesn't return anything");

        if (!functionEnv.returnType.isValidRHS(expression.type))
            throw fail(ctx, "return", "Cannot return type '" + expression.type +
                    "' from a functiоn which is expected to return '" + functionEnv.returnType + "'");

        return new OneInstruction(new Return(expression.instruction), null);
    }

    @Override
    public OneInstruction visitVariable_init(c4waParser.Variable_initContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (rhs == null)
            throw fail(ctx, "init", "RHS was not parsed");

        if (rhs.type == null)
            throw fail(ctx,"init", "RHS expression has no type");

        if (variableDecl.type.is_i64() && rhs.type.is_i32() && rhs.instruction instanceof Const) {
            rhs = new OneInstruction(new Const((((Const) rhs.instruction).longValue)), variableDecl.type);
        }
        else if (variableDecl.type.is_f64() && rhs.type.is_f32() && rhs.instruction instanceof Const) {
            rhs = new OneInstruction(new Const((((Const) rhs.instruction).doubleValue)), variableDecl.type);
        }

        if (!variableDecl.type.isValidRHS(rhs.type))
            throw fail(ctx,"init", "Expression of type " + rhs.type + " cannot be assigned to variable of type " + variableDecl.type);

        functionEnv.registerVar(variableDecl.name, variableDecl.type, false);

        return new OneInstruction(new SetLocal(variableDecl.name, rhs.instruction), null);
    }

    @Override
    public OneInstruction visitSimple_increment(c4waParser.Simple_incrementContext ctx) {
        String name = ctx.ID().getText();

        CType type = functionEnv.varType.get(name);
        boolean is_global = type == null;
        if (is_global) {
            VariableDecl decl = moduleEnv.varDecl.get(name);

            if (decl == null)
                throw fail(ctx, "assignment", "Variable '" + name + "' is not defined");

            if (!decl.mutable)
                throw fail(ctx, "assignment", "Global variable '" + name + "' is not mutable");

            type = decl.type;
        }

        String op;
        OneInstruction rhs;

        if (ctx.PLUSPLUS() != null) {
            op = "+";
            rhs = new OneInstruction(new Const(type.asNumType(), 1), type);
        }
        else if (ctx.MINUSMINUS() != null) {
            op = "-";
            rhs = new OneInstruction(new Const(type.asNumType(), 1), type);
        }
        else {
            op = ctx.op.getText().substring(0, ctx.op.getText().length() - 1);
            rhs = (OneInstruction) visit(ctx.expression());
        }

        OneInstruction binaryOp = binary_op(ctx, accessVariable(ctx, name), rhs, op);

        if (functionEnv.varType.containsKey(name))
            return new OneInstruction(new SetLocal(name, binaryOp.instruction), null);
        else {
            VariableDecl decl = moduleEnv.varDecl.get(name);

            if (!decl.mutable)
                throw fail(ctx, "assignment", "Global variable '" + name + "' is not mutable");

            return new OneInstruction(new SetGlobal(name, binaryOp.instruction), null);
        }
    }

    @Override
    public OneInstruction visitSimple_assignment(c4waParser.Simple_assignmentContext ctx) {
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (rhs.type == null)
            throw fail(ctx, "assignment", "RHS expression has no type");

        int iGlobal = -1;
        String[] names = ctx.ID().stream().map(ParseTree::getText).toArray(String[]::new);

        for (int i = 0; i < names.length; i ++) {
            CType type = functionEnv.varType.get(names[i]);
            if (type == null) {
                VariableDecl decl = moduleEnv.varDecl.get(names[i]);

                if (decl == null)
                    throw fail(ctx, "assignment", "Variable '" + names[i] + "' is not defined");

                if (!decl.mutable)
                    throw fail(ctx, "assignment", "Global variable '" + names[i] + "' is not mutable");

                if (iGlobal >= 0)
                    throw fail(ctx, "assignment", "Can have at most one global variable in chain assignment; found at least two, '" +
                            names[iGlobal] + "' and '" + names[i] + "'");

                iGlobal = i;

                type = decl.type;
            }
            if (type == null)
                throw fail(ctx, "assignment", "Variable '" + names[i] + "' is not defined");
            if (!type.isValidRHS(rhs.type))
                throw fail(ctx, "init", "Expression of type " + rhs.type + " cannot be assigned to variable '" + names[i] + "' of type " + type);
        }

        Instruction res = rhs.instruction;
        for (int i = 0; i < names.length; i++) {
            if (i == iGlobal)
                continue;

            if (iGlobal >= 0 || i < names.length - 1)
                res = new TeeLocal(names[i], res);
            else
                res = new SetLocal(names[i], res);
        }

        if (iGlobal >= 0)
            res = new SetGlobal(names[iGlobal], res);

        return new OneInstruction(res, null);
    }

    @Override
    public OneInstruction visitComplex_increment(c4waParser.Complex_incrementContext ctx) {
        String op = ctx.op.getText().substring(0, ctx.op.getText().length() - 1);
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());
        OneInstruction lhs = (OneInstruction) visit(ctx.lhs());

        throw fail(ctx, "increment", "complex increment not yet implemented");
    }

    @Override
    public OneInstruction visitComplex_assignment(c4waParser.Complex_assignmentContext ctx) {
        OneInstruction lhs = (OneInstruction) visit(ctx.lhs());
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (lhs.type == null)
            throw fail(ctx, "assign", "LHS has no type");
        if (rhs.type == null)
            throw fail(ctx, "assign", "RHS has no type");

        if (!lhs.type.isValidRHS(rhs.type))
            throw fail(ctx, "assign", "Expression of type " + rhs.type + " cannot be assigned to '" + lhs.type + "'");

        if (lhs.type.same(CType.CHAR))
            return new OneInstruction(new Store(lhs.type.asNumType(), 8, lhs.instruction, rhs.instruction), null);
        else if (lhs.type.same(CType.SHORT))
            return new OneInstruction(new Store(lhs.type.asNumType(), 16, lhs.instruction, rhs.instruction), null);
        else
            return new OneInstruction(new Store(lhs.type.asNumType(), lhs.instruction, rhs.instruction), null);
    }

    @Override
    public NoOp visitMult_variable_decl(c4waParser.Mult_variable_declContext ctx) {
        for (var v : ctx.variable_with_modifiers()) {
            VariableWrapper variableWrapper = (VariableWrapper) visit(v);
            CType type = (CType) visit(ctx.primitive());

            for (int i = 0; i < variableWrapper.ref_level; i++)
                type = type.make_pointer_to();
            functionEnv.registerVar(variableWrapper.name, type, false);
        }
        return new NoOp();
    }

    @Override
    public OneInstruction visitExpression_sizeof_type(c4waParser.Expression_sizeof_typeContext ctx) {
        CType type = (CType) visit(ctx.variable_type());
        return new OneInstruction(new Const(type.size()), CType.INT);
    }

    @Override
    public OneInstruction visitExpression_sizeof_exp(c4waParser.Expression_sizeof_expContext ctx) {
        OneInstruction e = (OneInstruction) visit(ctx.expression());

        return new OneInstruction(new Const(e.type.size()), CType.INT);
    }

    @Override
    public OneInstruction visitLhs_dereference(c4waParser.Lhs_dereferenceContext ctx) {
        OneInstruction ptr = (OneInstruction) visit(ctx.expression());

        if (ptr.type == null)
            throw fail(ctx, "dereference", "trying to dereference with no type");

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "dereference", "trying to dereference '" + ptr.type + "' which is not a reference");

        return new OneInstruction(ptr.instruction, type);
    }

    @Override
    public OneInstruction visitExpression_struct_member(c4waParser.Expression_struct_memberContext ctx) {
        OneInstruction ptr = (OneInstruction) visit(ctx.expression());

        if (ptr.type == null || ptr.type.deref() == null || !(ptr.type.deref() instanceof Struct))
            throw fail(ctx, "struct_member", "'" + ptr.type + "' is not a structure pointer");

        String mem = ctx.ID().getText();
        Struct c_struct = (Struct) ptr.type.deref();
        Struct.Var mInfo = c_struct.m.get(mem);

        if (mInfo == null)
            throw fail(ctx, "struct_member", "No member '" + mem + "' in " + c_struct);

        CType type = mInfo.type;

        Instruction memAddress = new Add(NumType.I32, ptr.instruction, new Const(mInfo.offset));

        if (type.same(CType.CHAR))
            return new OneInstruction(new Load(type.asNumType(), 8, type.is_signed(), memAddress), type);
        else if (type.same(CType.SHORT))
            return new OneInstruction(new Load(type.asNumType(), 16, type.is_signed(), memAddress), type);
        else
            return new OneInstruction(new Load(type.asNumType(), memAddress), type);
    }

    @Override
    public OneInstruction visitLhs_struct_member(c4waParser.Lhs_struct_memberContext ctx) {
        OneInstruction ptr = (OneInstruction) visit(ctx.expression());

        if (ptr.type == null || ptr.type.deref() == null || !(ptr.type.deref() instanceof Struct))
            throw fail(ctx, "struct_member", "'" + ptr.type + "' is not a structure pointer");

        String mem = ctx.ID().getText();
        Struct c_struct = (Struct) ptr.type.deref();
        Struct.Var mInfo = c_struct.m.get(mem);

        if (mInfo == null)
            throw fail(ctx, "struct_member", "No member '" + mem + "' in " + c_struct);

        return new OneInstruction(new Add(NumType.I32, ptr.instruction, new Const(mInfo.offset)), mInfo.type);
    }

    @Override
    public OneInstruction visitLhs_index(c4waParser.Lhs_indexContext ctx) {
        OneInstruction ptr = (OneInstruction) visit(ctx.ptr);
        OneInstruction idx = (OneInstruction) visit(ctx.idx);

        if (ptr.type == null)
            throw fail(ctx, "index", "trying to dereference with no type");

        if (idx.type == null)
            throw fail(ctx, "index", "index must be INT, got void");

        if (!idx.type.same(CType.INT))
            throw fail(ctx, "index", "index must be INT, got '" + idx.type + "'");

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "index", "trying to dereference '" + ptr.type + "' which is not a reference");

        if (type.size() == 1)
            return new OneInstruction(new Add(NumType.I32, ptr.instruction, idx.instruction), type);
        else
            return new OneInstruction(new Add(NumType.I32, ptr.instruction,
                    new Mul(NumType.I32, idx.instruction, new Const(type.size()))), type);
    }

    @Override
    public OneInstruction visitExpression_index(c4waParser.Expression_indexContext ctx) {
        OneInstruction ptr = (OneInstruction) visit(ctx.ptr);
        OneInstruction idx = (OneInstruction) visit(ctx.idx);

        if (ptr.type == null)
            throw fail(ctx, "index", "trying to dereference with no type");

        if (idx.type == null)
            throw fail(ctx, "index", "index must be INT, got void");

        if (!idx.type.same(CType.INT))
            throw fail(ctx, "index", "index must be INT, got '" + idx.type + "'");

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "index", "trying to dereference '" + ptr.type + "' which is not a reference");

        Instruction memAddress = (type.size() == 1)
                ? new Add(NumType.I32, ptr.instruction, idx.instruction)
                : new Add(NumType.I32, ptr.instruction, new Mul(NumType.I32, idx.instruction, new Const(type.size())));


        if (type.same(CType.CHAR))
            return new OneInstruction(new Load(type.asNumType(), 8, type.is_signed(), memAddress), type);
        else if (type.same(CType.SHORT))
            return new OneInstruction(new Load(type.asNumType(), 16, type.is_signed(), memAddress), type);
        else
            return  new OneInstruction(new Load(type.asNumType(), memAddress), type);
    }

    @Override
    public OneInstruction visitExpression_binary_cmp(c4waParser.Expression_binary_cmpContext ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneInstruction visitExpression_binary_add(c4waParser.Expression_binary_addContext ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneInstruction visitExpression_binary_mult(c4waParser.Expression_binary_multContext ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneInstruction visitExpression_binary_or(c4waParser.Expression_binary_orContext ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneInstruction visitExpression_binary_and(c4waParser.Expression_binary_andContext ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    private OneInstruction binary_op (ParserRuleContext ctx, OneInstruction arg1, OneInstruction arg2, String op) {
        if (arg1 == null)
            throw fail(ctx, "Expression", "1-st arg not parsed");
        if (arg2 == null)
            throw fail(ctx, "Expression", "2-nd arg not parsed");
        if (arg1.type == null)
            throw fail(ctx, "Expression", "1-st arg has no type");
        if (arg2.type == null)
            throw fail(ctx, "Expression", "2-nd arg has no type");

        if (arg1.type.is_i64() && arg2.type.is_i32() && arg2.instruction instanceof Const) {
            arg2 = new OneInstruction(new Const((((Const) arg2.instruction).longValue)), arg1.type);
        }
        else if (arg2.type.is_i64() && arg1.type.is_i32() && arg1.instruction instanceof Const) {
            arg1 = new OneInstruction(new Const((((Const) arg1.instruction).longValue)), arg2.type);
        }
        else if (arg1.type.is_f64() && arg2.type.is_f32() && arg2.instruction instanceof Const) {
            arg2 = new OneInstruction(new Const((((Const) arg2.instruction).doubleValue)), arg1.type);
        }
        else if (arg2.type.is_f64() && arg1.type.is_f32() && arg1.instruction instanceof Const) {
            arg1 = new OneInstruction(new Const((((Const) arg1.instruction).doubleValue)), arg2.type);
        }

        NumType numType;
        CType resType;

        if (arg2.type.same(CType.INT) && arg1.type.deref() != null) {
            CType type = arg1.type.deref();
            return new OneInstruction((type.size() == 1)
                    ? new Add(NumType.I32, arg1.instruction, arg2.instruction)
                    : new Add(NumType.I32, arg1.instruction, new Mul(NumType.I32, arg2.instruction, new Const(type.size()))),
                    arg1.type);
        }
        else if (arg1.type.same(CType.INT) && arg2.type.deref() != null) {
            CType type = arg2.type.deref();
            return new OneInstruction((type.size() == 1)
                    ? new Add(NumType.I32, arg2.instruction, arg1.instruction)
                    : new Add(NumType.I32, arg2.instruction, new Mul(NumType.I32, arg1.instruction, new Const(type.size()))),
                    arg2.type);
        }

        else if (arg1.type.is_i32() && arg2.type.is_i32()) {
            numType = NumType.I32;
            resType = CType.INT;
        } else if (arg1.type.is_i64() && arg2.type.is_i64()) {
            numType = NumType.I64;
            resType = CType.LONG;
        } else if (arg1.type.is_f32() && arg2.type.is_f32()) {
            numType = NumType.F32;
            resType = CType.FLOAT;
        } else if (arg1.type.is_f64() && arg2.type.is_f64()) {
            numType = NumType.F64;
            resType = CType.DOUBLE;
        }
        else
            throw fail(ctx, "binary operation '" + op + "' ", "Types " + arg1.type + " and " + arg2.type +
                    " are incompatible");

        if (arg1.type.is_signed() != arg2.type.is_signed())
            throw fail(ctx, "binary operation '" + op + "'", "cannot combined signed and unsigned types");

        Instruction res;
        if ("+".equals(op))
            res = new Add(numType, arg1.instruction, arg2.instruction);
        else if ("-".equals(op))
            res = new Sub(numType, arg1.instruction, arg2.instruction);
        else if ("*".equals(op))
            res = new Mul(numType, arg1.instruction, arg2.instruction);
        else if ("/".equals(op))
            res = new Div(numType, arg1.type.is_signed(), arg1.instruction, arg2.instruction);
        else if ("%".equals(op))
            res = new Rem(numType, arg1.type.is_signed(), arg1.instruction, arg2.instruction);
        else if (List.of("<", "<=", ">", ">=").contains(op))
            res = new Cmp(numType, op.charAt(0) == '<', op.length() == 2, arg1.type.is_signed(),
                    arg1.instruction, arg2.instruction);
        else if (op.equals("==") && arg1.instruction instanceof Const && ((Const)arg1.instruction).longValue == 0)
            res = new Eqz(numType, arg2.instruction);
        else if (op.equals("==") && arg2.instruction instanceof Const && ((Const)arg2.instruction).longValue == 0)
            res = new Eqz(numType, arg1.instruction);
        else if (List.of("==", "!=").contains(op))
            res = new Cmp(numType, op.charAt(0) == '=', arg1.instruction, arg2.instruction);
        else if ("&&".equals(op))
            res = new And(numType, arg1.instruction, arg2.instruction);
        else if ("||".equals(op))
            res = new Or(numType, arg1.instruction, arg2.instruction);
        else
            throw fail(ctx, "binary operation", "Instruction '" + op + "' not recognized");

        return new OneInstruction(res, resType);
    }

    @Override
    public OneInstruction visitExpression_unary_op(c4waParser.Expression_unary_opContext ctx) {
        String op = ctx.op.getText();
        OneInstruction exp = (OneInstruction) visit(ctx.expression());

        if (exp.type == null)
            throw fail(ctx, "unary_op", "expression has no type");

        if (op.equals("-")) {
            if (exp.type.is_int())
                return new OneInstruction(new Sub(exp.type.asNumType(), new Const(0), exp.instruction), exp.type);
            else
                return new OneInstruction(new Neg(exp.type.asNumType(), exp.instruction), exp.type);
        }
        else if (op.equals("!")) {
            return new OneInstruction(exp.instruction.Not(exp.type.asNumType()), exp.type);
        }
        else if (op.equals("*")) {
            CType type = exp.type.deref();
            if (type == null)
                throw fail(ctx, "unary_op", "Trying to dereference '" + exp.type + "', must be a pointer");

            if (type.same(CType.CHAR))
                return new OneInstruction(new Load(type.asNumType(), 8, type.is_signed(), exp.instruction), type);
            else if (type.same(CType.SHORT))
                return new OneInstruction(new Load(type.asNumType(), 16, type.is_signed(), exp.instruction), type);
            else
                return new OneInstruction(new Load(type.asNumType(),exp.instruction), type);
        }
        else
            throw fail(ctx, "unary_op", "Operation '" + op + "' not recognized");
    }

    @Override
    public OneInstruction visitExpression_alloc(c4waParser.Expression_allocContext ctx) {
        OneInstruction memptr = (OneInstruction) visit(ctx.memptr);
        //OneInstruction count = (OneInstruction) visit(ctx.count);
        CType type = (CType) visit(ctx.variable_type());

        if (!memptr.type.is_i32())
            throw fail(ctx, "alloc", "'" + memptr.type + "' won't work for alloc, must have int");

        int memOffset = ModuleEnv.DATA_OFFSET + ModuleEnv.DATA_LENGTH;
        if (memptr.instruction instanceof Const)
            return new OneInstruction(new Const(memOffset + (int)((Const)memptr.instruction).longValue), type.make_pointer_to());
        else
            return new OneInstruction(new Add(NumType.I32, memptr.instruction, new Const(memOffset)), type.make_pointer_to());
    }

    @Override
    public OneInstruction visitExpression_cast(c4waParser.Expression_castContext ctx) {
        OneInstruction exp = (OneInstruction) visit(ctx.expression());
        CType castToType = (CType) visit(ctx.variable_type());

        boolean signed;
        if (exp.type.is_int())
            signed = exp.type.is_signed();
        else if (castToType.is_int())
            signed = castToType.is_signed();
        else
            signed = true;

        return new OneInstruction(new GenericCast(exp.type.asNumType(), castToType.asNumType(), signed, exp.instruction), castToType);
    }

    @Override
    public OneInstruction visitExpression_variable(c4waParser.Expression_variableContext ctx) {
        return accessVariable(ctx, ctx.ID().getText());
    }

    private OneInstruction accessVariable(ParserRuleContext ctx, String name) {
        CType type = functionEnv.varType.get(name);
        if (type != null)
            return new OneInstruction(new GetLocal(name), type);

        VariableDecl globalDecl = moduleEnv.varDecl.get(name);

        if (globalDecl != null)
            return new OneInstruction(new GetGlobal(name), globalDecl.type);

        throw fail(ctx, "variable", "not defined");
    }

    @Override
    public OneInstruction visitExpression_const(c4waParser.Expression_constContext ctx) {
        return parseConstant(ctx, ctx.CONSTANT().getText());
    }

    @Override
    public OneInstruction visitExpression_string(c4waParser.Expression_stringContext ctx) {
        String str = unescape(ctx.STRING().getText());
        return new OneInstruction(new Const(moduleEnv.addString(str)), CType.INT);
    }

    @Override
    public CType visitInteger_primitive(c4waParser.Integer_primitiveContext ctx) {
        if (ctx.CHAR() != null)
            return CType.CHAR;
        else if (ctx.SHORT() != null)
            return CType.SHORT;
        else if (ctx.INT() != null)
            return CType.INT;
        else if (ctx.LONG() != null)
            return CType.LONG;
        else
            throw fail(ctx, "primitive", "Type " + ctx + " not implemented");
    }

    @Override
    public CType visitFloat_primitive(c4waParser.Float_primitiveContext ctx) {
        if (ctx.FLOAT() != null)
            return CType.FLOAT;
        else if (ctx.DOUBLE() != null)
            return CType.DOUBLE;
        else
            throw fail(ctx, "primitive", "Type " + ctx + " not implemented");
    }

    @Override
    public CType visitStruct_primitive(c4waParser.Struct_primitiveContext ctx) {
        String name = ctx.ID().getText();

        Struct c_struct = moduleEnv.structs.get(name);

        if (c_struct == null)
            throw fail(ctx, "struct", "No structure name '" + name + "'");

        return c_struct;
    }

    private OneInstruction parseConstant(ParserRuleContext ctx, String textOfConstant) {
        try {
            return new OneInstruction(new Const(Integer.parseInt(textOfConstant)), CType.INT);
        } catch (NumberFormatException ignored) {
        }

        try {
            return new OneInstruction(new Const(Long.parseLong(textOfConstant)), CType.LONG);
        } catch (NumberFormatException ignored) {
        }

        try {
            return new OneInstruction(new Const(Double.parseDouble(textOfConstant)), CType.DOUBLE);
        } catch (NumberFormatException ignored) {
        }

        throw fail(ctx, "const", "'" + textOfConstant + "' cannot be parsed");
    }

    private Const parseConstant(ParserRuleContext ctx, CType ctype, String textOfConstant) {
        try {
            if (ctype == CType.INT)
                return new Const(Integer.parseInt(textOfConstant));
            else if (ctype == CType.LONG)
                return new Const(Long.parseLong(textOfConstant));
            else if (ctype == CType.FLOAT)
                return new Const(Float.parseFloat(textOfConstant));
            else
                return new Const(Double.parseDouble(textOfConstant));

        } catch (NumberFormatException err) {
            throw fail(ctx, "const", "'" + textOfConstant + "' cannot be parsed as " + ctype);
        }
    }

    private Partial visit(ParserRuleContext ctx) {
        if (ctx == null)
            return null;
        return super.visit(ctx);
    }
    /*
    Default implementation always returns last result, even if null
    Change it to return first not-null
    This will save us from implementing visitor overrides for elements with only one non-trivial child
     */
    @Override
    protected Partial aggregateResult(Partial aggregate, Partial nextResult) {
        if (aggregate != null)
            return aggregate;
        else
            return nextResult;
    }

    static String unescape(String str) {
        StringBuilder b = new StringBuilder();
        int N = str.length();
        for (int i = 1; i < N - 1; i ++) {
            if (str.charAt(i) == '\\' && "\\\"".contains(String.valueOf(str.charAt(i+1))))
                i ++;

            b.append(str.charAt(i));
        }
        return b.toString();
    }

    private RuntimeException fail(ParserRuleContext ctx, String desc, String error) {
        return new RuntimeException("[" + ctx.start.getLine() + ":" +
                ctx.start.getCharPositionInLine() + "] " + desc + " " + ctx.getText() + " : " + error);
    }

}
