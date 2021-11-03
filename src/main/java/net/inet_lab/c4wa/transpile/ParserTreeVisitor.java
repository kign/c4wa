package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class ParserTreeVisitor extends c4waBaseVisitor<Partial> {
    private FunctionEnv functionEnv;
    private ModuleEnv moduleEnv;
    final private Deque<BlockEnv> blockStack;

    public ParserTreeVisitor() {
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

    static class InstructionList implements Partial {
        final OneInstruction[] instructions;
        InstructionList(OneInstruction[] instructions) {
            this.instructions = instructions;
        }
    }

    static class OneFunction implements Partial {
        final FunctionEnv func;
        final c4waParser.Big_blockContext code;
        OneFunction(FunctionEnv func, c4waParser.Big_blockContext code) {
            this.func = func;
            this.code = code;
        }
    }

    static class BlockEnv {
        int offset;
        final int start_offset;
        final List<Instruction> prefix;
        BlockEnv (int offset) {
            this.start_offset = offset;
            this.offset = offset;
            prefix = new ArrayList<>();
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

        // moduleEnv.addDeclaration(functionEnv.makeDeclaration());

        return new OneFunction(functionEnv, ctx.big_block());
        /*
        functionEnv.addInstructions(((InstructionList)visit(ctx.big_block())).instructions);
        return functionEnv;
        */
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
        if (ctx.big_block() != null)
            return (InstructionList) visit(ctx.big_block());
        else {
            OneInstruction[] instructions = new OneInstruction[1];
            instructions[0] = (OneInstruction) visit(ctx.element());
            return new InstructionList(instructions);
        }
    }

    @Override
    public InstructionList visitBig_block(c4waParser.Big_blockContext ctx) {
        BlockEnv blockEnv = new BlockEnv(functionEnv.getMemOffset());
        blockStack.push(blockEnv);
        List<OneInstruction> res = new ArrayList<>();

        int idx = 0;
        for (var blockElem : ctx.element()) {
            idx ++;
            OneInstruction instruction = (OneInstruction) visit(blockElem);
            if (instruction == null)
                throw fail(ctx, "block", "Instruction number " + idx + " was not parsed");
            for (var i : blockEnv.prefix)
                res.add(new OneInstruction(i, null));
            blockEnv.reset();
            res.add(instruction);
        }

        blockStack.pop();
        return new InstructionList(res.toArray(OneInstruction[]::new));
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
        OneInstruction expression = (OneInstruction) visit(ctx.expression());

        if (expression.type == null)
            throw fail(ctx,"init", "RHS expression has no type");

        if (!variableDecl.type.isValidRHS(expression.type))
            throw fail(ctx,"init", "Expression of type " + expression.type + " cannot be assigned to variable of type " + variableDecl.type);

        functionEnv.registerVar(variableDecl.name, variableDecl.type, false);

        return new OneInstruction(new SetLocal(variableDecl.name, expression.instruction), null);
    }


    @Override
    public OneInstruction visitExpression_binary_op1(c4waParser.Expression_binary_op1Context ctx) {
        OneInstruction arg1 = (OneInstruction) visit(ctx.expression());
        OneInstruction arg2 = (OneInstruction) visit(ctx.arg2());
        String op = ctx.BINARY_OP1().getText();

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
        }
        else if (arg1.type.is_i64() && arg2.type.is_i64()) {
            numType = NumType.I64;
            resType = CType.LONG;
        }
        else if (arg1.type.is_f32() && arg2.type.is_f32()) {
            numType = NumType.F32;
            resType = CType.FLOAT;
        }
        else if (arg1.type.is_f64() && arg2.type.is_f64()) {
            numType = NumType.F64;
            resType = CType.DOUBLE;
        }
        else
            throw fail(ctx, "binary operation", "Types " + arg1.type + " and " + arg2.type +
                    " are incompatible for binary operation <" + op + ">");

        Instruction res;
        if ("+".equals(op))
            res = new Add(numType, arg1.instruction, arg2.instruction);
        else if ("-".equals(op))
            res = new Sub(numType, arg1.instruction, arg2.instruction);
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
