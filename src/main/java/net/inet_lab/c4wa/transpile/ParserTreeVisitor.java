package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Arrays;
import java.util.Objects;

public class ParserTreeVisitor extends c4waBaseVisitor<Partial> {
    private FunctionEnv functionEnv;
    private ModuleEnv moduleEnv;

    public ParserTreeVisitor() {
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
    }

    static class OneInstruction implements Partial {
        final Instruction instruction;
        final CType type;

        OneInstruction(Instruction instruction, CType type) {
            this.instruction = instruction;
            this.type = type;
        }

        OneInstruction(Instruction instruction) {
            this.instruction = instruction;
            this.type = null;
        }
    }

    static class InstructionList implements Partial {
        final Instruction[] instructions;
        InstructionList(Instruction[] instructions) {
            this.instructions = instructions;
        }
    }

    @Override
    public ModuleEnv visitModule(c4waParser.ModuleContext ctx) {
        moduleEnv = new ModuleEnv();

        moduleEnv.addFunctions(ctx.func().stream().map(this::visit).toArray(FunctionEnv[]::new));

        return moduleEnv;
    }

    @Override
    public Partial visitFunc(c4waParser.FuncContext ctx) {
        VariableDecl funcDecl = (VariableDecl) visit(ctx.variable_decl());
        ParamList paramList = (ParamList) visit(ctx.param_list());

        functionEnv = new FunctionEnv(funcDecl.name, funcDecl.type,
                ctx.EXTERN() != null,
                Arrays.stream(paramList.paramList).map(x -> x.name).toArray(String[]::new));

        Arrays.stream(paramList.paramList).forEach(x -> functionEnv.registerVar(x.name, x.type));

        functionEnv.addInstructions(((InstructionList)visit(ctx.big_block())).instructions);

        return functionEnv;
    }

    @Override
    public ParamList visitParam_list(c4waParser.Param_listContext ctx) {
        return new ParamList(ctx.variable_decl().stream().map(this::visit).toArray(VariableDecl[]::new));
    }

    @Override
    public VariableDecl visitVariable_decl_primitive(c4waParser.Variable_decl_primitiveContext ctx) {
        return new VariableDecl((CType) visit(ctx.primitive()), ctx.ID().getText());
    }

    @Override
    public InstructionList visitBlock(c4waParser.BlockContext ctx) {
        if (ctx.big_block() != null)
            return (InstructionList) visit(ctx.big_block());
        else {
            Instruction[] instructions = new Instruction[1];
            instructions[0] = ((OneInstruction) visit(ctx.element())).instruction;
            return new InstructionList(instructions);
        }
    }

    @Override
    public InstructionList visitBig_block(c4waParser.Big_blockContext ctx) {
        return new InstructionList(ctx.element().stream()
                .map(this::visit)
                .filter(Objects::nonNull)
                .map(x -> ((OneInstruction)x).instruction)
                .toArray(Instruction[]::new));
    }

    @Override
    public OneInstruction visitReturn_expression(c4waParser.Return_expressionContext ctx) {
        return new OneInstruction(new Return(((OneInstruction)visit(ctx.expression())).instruction));
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
            throw new RuntimeException("[" + ctx + "] Types " + arg1.type + " and " + arg2.type +
                    " are incompatible for binary operation <" + op + ">");

        Instruction res;
        if ("+".equals(op))
            res = new Add(numType, arg1.instruction, arg2.instruction);
        else if ("-".equals(op))
            res = new Sub(numType, arg1.instruction, arg2.instruction);
        else
            throw new RuntimeException("[" + ctx + "] Instruction " + op + " not recognized");

        return new OneInstruction(res, resType);
    }

    @Override
    public OneInstruction visitExpression_variable(c4waParser.Expression_variableContext ctx) {
        String name = ctx.ID().getText();
        CType type = functionEnv.locals.get(name);
        if (type == null)
            throw fail(ctx, "variable", "not defined");

        return new OneInstruction(new GetLocal(name), type);
    }


    @Override
    public CType visitPrimitive(c4waParser.PrimitiveContext ctx) {
        if (ctx.CHAR() != null)
            return CType.CHAR;
        else if (ctx.SHORT() != null)
            return CType.SHORT;
        else if (ctx.INT() != null)
            return CType.INT;
        else if (ctx.LONG() != null)
            return CType.LONG;
        else
            throw new RuntimeException("Type " + ctx + " not implemented");
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

    private RuntimeException fail(ParserRuleContext ctx, String desc, String error) {
        return new RuntimeException(ctx.start.getLine() + ":" +
                ctx.start.getCharPositionInLine() + "] " + desc + ctx.getText() + ", " + error);
    }

}
