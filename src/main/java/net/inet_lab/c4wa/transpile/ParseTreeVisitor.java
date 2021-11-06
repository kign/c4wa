package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;

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

    static class VariableDecl implements Partial {
        final CType type;
        final String name;
        VariableDecl(CType type, String name) {
            this.type = type;
            this.name = name;
        }
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
        InstructionList(OneInstruction[] instructions) {
            this.instructions = instructions;
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
        }
    }

    @Override
    public ModuleEnv visitModule(c4waParser.ModuleContext ctx) {
        moduleEnv = new ModuleEnv();

        OneFunction[] functions = ctx.func().stream().map(this::visit).toArray(OneFunction[]::new);

        for (var globalDecl : ctx.global_decl())
            moduleEnv.addDeclaration((FunctionDecl) visit(globalDecl));

        for (var oneFunc : functions)
            moduleEnv.addDeclaration(oneFunc.func.makeDeclaration());

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
    public FunctionDecl visitGlobal_decl_function(c4waParser.Global_decl_functionContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());

        if (ctx.ELLIPSIS() != null)
            return new FunctionDecl(variableDecl.name, variableDecl.type, null, true, true);
        else {
            throw fail(ctx, "function declaration", "type list not yet implemented");
        }
    }

    @Override
    public OneFunction visitFunc(c4waParser.FuncContext ctx) {
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
            OneInstruction[] instructions = new OneInstruction[1];
            instructions[0] = (OneInstruction) visit(ctx.element());
            return new InstructionList(instructions);
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
        if (parent instanceof c4waParser.Element_do_whileContext)
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
        return new InstructionList(res.toArray(OneInstruction[]::new));
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
            throw fail(ctx, "do_while", "Expression '" + ctx.expression().getText() + "' has no type (any time would have worked in WASM as 'boolean', but there is none)");

        List<Instruction> body_elems = new ArrayList<>();
        for(var i : body.instructions)
            body_elems.add(i.instruction);

        body_elems.add(new BrIf(block_id_cont, condition.instruction));
        return new OneInstruction(new Loop(block_id_cont, body_elems.toArray(Instruction[]::new)), null);
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
                blockEnv.prefix.add(new Store(arg.type.asNumType(), new Const(blockEnv.getOffset()), arg.instruction));
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
        return new OneInstruction(new Return(((OneInstruction)visit(ctx.expression())).instruction), null);
    }

    @Override
    public OneInstruction visitVariable_init(c4waParser.Variable_initContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (rhs == null)
            throw fail(ctx, "init", "RHS was not parsed");

        if (rhs.type == null)
            throw fail(ctx,"init", "RHS expression has no type");

        if (!variableDecl.type.isValidRHS(rhs.type))
            throw fail(ctx,"init", "Expression of type " + rhs.type + " cannot be assigned to variable of type " + variableDecl.type);

        functionEnv.registerVar(variableDecl.name, variableDecl.type, false);

        return new OneInstruction(new SetLocal(variableDecl.name, rhs.instruction), null);
    }

    @Override
    public InstructionList visitSimple_assignment(c4waParser.Simple_assignmentContext ctx) {
        OneInstruction rhs = (OneInstruction) visit(ctx.expression());

        if (rhs.type == null)
            throw fail(ctx, "assignment", "RHS expression has no type");

        List<OneInstruction> assignments = new ArrayList<>();

        for (var v : ctx.ID()) {
            String name = v.getText();
            CType type = functionEnv.varType.get(name);
            if (type == null)
                throw fail(ctx, "assignment", "Variable '" + name + "' is not defined");
            if (!type.isValidRHS(rhs.type))
                throw fail(ctx, "init", "Expression of type " + rhs.type + " cannot be assigned to variable of type " + type);

            assignments.add(new OneInstruction(new SetLocal(name, rhs.instruction), null));
        }

        return new InstructionList(assignments.toArray(OneInstruction[]::new));
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
        else if (List.of("<", "<=", ">", ">=").contains(op))
            res = new Cmp(numType, op.charAt(0) == '<', op.length() == 2, arg1.type.is_signed(),
                    arg1.instruction, arg2.instruction);
        else
            throw fail(ctx, "binary operation", "Instruction " + op + " not recognized");

        return new OneInstruction(res, resType);
    }


    @Override
    public OneInstruction visitExpression_variable(c4waParser.Expression_variableContext ctx) {
        String name = ctx.ID().getText();
        CType type = functionEnv.varType.get(name);
        if (type == null)
            throw fail(ctx, "variable", "not defined");

        return new OneInstruction(new GetLocal(name), type);
    }

    @Override
    public OneInstruction visitExpression_const(c4waParser.Expression_constContext ctx) {
        String c = ctx.CONST().getText();

        try {
            return new OneInstruction(new Const(Integer.parseInt(c)), CType.INT);
        }
        catch(NumberFormatException ignored) {
        }

        try {
            return new OneInstruction(new Const(Long.parseLong(c)), CType.LONG);
        }
        catch(NumberFormatException ignored) {
        }

        try {
            return new OneInstruction(new Const(Double.parseDouble(c)), CType.DOUBLE);
        }
        catch(NumberFormatException ignored) {
        }

        throw fail(ctx, "const", "'" + c  + "' cannot be parsed");
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
