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
        ParamList(VariableDecl[] paramList) {
            this.paramList = paramList;
        }
        ParamList() {
            this.paramList = new VariableDecl[0];
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
        void markAsLoop(String block_id) {
            this.block_id = block_id;
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
        return new VariableDecl((CType) visit(ctx.variable_type()), ctx.ID().getText());
    }

    @Override
    public CType visitType_primitive(c4waParser.Type_primitiveContext ctx) {
        return (CType) visit(ctx.primitive());
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
                blockEnv.markAsLoop(functionEnv.getBlock());

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
            blockEnv.markAsLoop(functionEnv.getBlock());

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

        String block_id = functionEnv.pushBlock();
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
        OneInstruction prestat = (OneInstruction) visit(ctx.statement(0));

        OneInstruction condition = (OneInstruction) visit(ctx.expression());
        if (condition.type == null)
            throw fail(ctx, "for", "Expression '" + ctx.expression().getText() +
                    "' has no type (any type would have worked in WASM as 'boolean', but there is none)");

        OneInstruction poststat = (OneInstruction) visit(ctx.statement(1));

        String block_id = functionEnv.pushBlock();
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        InstructionList body = (InstructionList) visit(ctx.block());
        functionEnv.popBlock();

        List<Instruction> body_elems = new ArrayList<>();
        body_elems.add(new BrIf(block_id_break, condition.instruction.Not(condition.type.asNumType())));
        for (var i : body.instructions)
            body_elems.add(i.instruction);
        body_elems.add(poststat.instruction);
        body_elems.add(new Br(block_id_cont));

        return new OneInstruction(
                new Block(block_id_break, new Instruction[]{
                        prestat.instruction,
                        new Loop(block_id_cont, body_elems.toArray(Instruction[]::new))}),
                null);
    }

    @Override
    public OneInstruction visitElement_break_continue_if(c4waParser.Element_break_continue_ifContext ctx) {
        throw fail(ctx, "break if", "not implemented");
    }

    @Override
    public OneInstruction visitElement_break_continue(c4waParser.Element_break_continueContext ctx) {
        boolean is_break = ctx.BREAK() != null;
        BlockEnv blockEnv = null;
        for (var b: blockStack)
            if (b.block_id != null) {
                blockEnv = b;
                break;
            }

        if (blockEnv == null)
            throw fail(ctx, is_break?"break":"continue", "cannot find eligible block");

        String ref = blockEnv.block_id + (is_break?BREAK_SUFFIX:CONT_SUFFIX);
        if (is_break)
            blockEnv.need_block = true;

        return new OneInstruction(new Br(ref),null);
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

        return new OneInstruction(new IfThenElse(condition.instruction, thenList.extract(), (elseList == null)?null:elseList.extract()),
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

        InstructionList args = (InstructionList) visit(ctx.arg_list());
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
    public OneInstruction visitSimple_assignment(c4waParser.Simple_assignmentContext ctx) {
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (rhs.type == null)
            throw fail(ctx, "assignment", "RHS expression has no type");

        List<OneInstruction> assignments = new ArrayList<>();

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
    public NoOp visitMult_variable_decl(c4waParser.Mult_variable_declContext ctx) {
        CType type = (CType) visit(ctx.variable_type());
        for (var v : ctx.ID())
            functionEnv.registerVar(v.getText(), type, false);
        return new NoOp();
    }

    @Override
    public OneInstruction visitExpression_binary_op0(c4waParser.Expression_binary_op0Context ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        String op = ctx.BINARY_OP0().getText();

        return binary_op(ctx, arg1, arg2, op);
    }

    @Override
    public OneInstruction visitExpression_binary_op1(c4waParser.Expression_binary_op1Context ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        String op = ctx.BINARY_OP1().getText();

        return binary_op(ctx, arg1, arg2, op);
    }

    @Override
    public OneInstruction visitExpression_binary_op2(c4waParser.Expression_binary_op2Context ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression(0));
        OneInstruction arg2 = (OneInstruction) visit(ctx.expression(1));
        String op = ctx.BINARY_OP2().getText();

        return binary_op(ctx, arg1, arg2, op);
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
        if (arg1.type.is_i32() && arg2.type.is_i32()) {
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
        } else
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
        else
            throw fail(ctx, "binary operation", "Instruction '" + op + "' not recognized");

        return new OneInstruction(res, resType);
    }

    @Override
    public OneInstruction visitExpression_unary_op(c4waParser.Expression_unary_opContext ctx) {
        String op = ctx.UNARY_OP().getText();
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
        else
            throw fail(ctx, "unary_op", "Operation '" + op + "' not recognized");
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
        String name = ctx.ID().getText();
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
