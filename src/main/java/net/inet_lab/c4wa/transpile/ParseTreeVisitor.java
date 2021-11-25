package net.inet_lab.c4wa.transpile;

import java.util.*;
import org.jetbrains.annotations.NotNull;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;


public class ParseTreeVisitor extends c4waBaseVisitor<Partial> {
    private FunctionEnv functionEnv;
    private ModuleEnv moduleEnv;
    final private Deque<BlockEnv> blockStack;
    final private Properties prop;

    final private static String CONT_SUFFIX = "_continue";
    final private static String BREAK_SUFFIX = "_break";
    static final private boolean print_stack_trace_on_errors = false;

    public ParseTreeVisitor(Properties prop) {
        blockStack = new ArrayDeque<>();
        this.prop = prop;
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

    static class LocalVariable implements Partial {
        final String name;
        final int ref_level;
        final Expression size;

        LocalVariable(String name, int ref_level, Expression size) {
            this.name = name;
            this.ref_level = ref_level;
            this.size = size;
        }
    }

    static class StructMember implements Partial {
        final String name;
        final int ref_level;
        final Integer size;

        StructMember(String name, int ref_level, Integer size) {
            this.name = name;
            this.ref_level = ref_level;
            this.size = size;
        }
    }

    static class StructDefinition implements Partial {
        final String name;
        final Struct.VarInput[] members;

        StructDefinition(Struct.VarInput[] members) {
            this.name = null;
            this.members = members;
        }

        StructDefinition(String name, Struct.VarInput[] members) {
            this.name = name;
            this.members = members;
        }
    }

    static class OneInstruction implements Partial {
        final Instruction instruction;

        OneInstruction(Instruction instruction) {
            this.instruction = instruction;
        }
    }

    static class OneExpression implements Partial {
        final Expression expression;
        @NotNull final CType type;

        OneExpression(Expression expression, @NotNull CType type) {
            this.expression = expression.comptime_eval();
            this.type = type;
        }
    }

    static class NoOp implements Partial {
    }

    static class InstructionList implements Partial {
        final Instruction[] instructions;
        boolean need_block;
        InstructionList(Instruction[] instructions, boolean need_block) {
            this.instructions = instructions;
            this.need_block = need_block;
        }
    }

    static class ExpressionList implements Partial {
        final OneExpression[] expressions;

        ExpressionList() {
            this.expressions = new OneExpression[0];
        }

        ExpressionList(OneExpression[] expressions) {
            this.expressions = expressions;
        }
    }

    static class DelayedList extends Instruction_Delayed {
        final Instruction[] instructions;
        DelayedList(List<Instruction> instructions) {
            this.instructions = instructions.toArray(Instruction[]::new);
        }

        @Override
        public String toString() {
            return "PreparedList";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            return instructions;
        }
    }

    static class DelayedReturn extends Instruction_Delayed {
        final Return instruction;
        DelayedReturn(Expression arg) {
            instruction = new Return(arg);
        }

        DelayedReturn() {
            instruction = new Return();
        }

        @Override
        public String toString() {
            return "PreparedReturn";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            if (functionEnv.uses_stack)
                return new Instruction[]{new SetGlobal(ModuleEnv.STACK_VAR_NAME, new GetLocal(NumType.I32, FunctionEnv.STACK_ENTRY_VAR)),
                        instruction};
            else
                return new Instruction[] {instruction};
        }
    }

    static class DelayedLocalDefinition extends Instruction_Delayed {
        final String name;
        DelayedLocalDefinition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "DelayedLocalDefinition('" + name + "')";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            VariableDecl decl = functionEnv.variables.get(name);
            assert decl != null;
            if (decl.inStack) {
                GetGlobal stack = new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME);
                return new Instruction[]{new SetLocal(name, stack), new SetGlobal(ModuleEnv.STACK_VAR_NAME, new Add(NumType.I32, stack, new Const(decl.type.size())))};
            }
            else
                return new Instruction[0];
        }
    }

    static class DelayedAssignment extends Instruction_Delayed {
        final String[] names;
        final Expression rhs;
        final CType type;
        final ParserRuleContext ctx;

        DelayedAssignment(ParserRuleContext ctx, String[] names, Expression rhs, CType type) {
            this.ctx = ctx;
            this.names = names;
            this.rhs = rhs;
            this.type = type;
        }

        @Override
        public String toString() {
            return "DelayedAssignment(" + String.join(",", names)  + ")";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            ModuleEnv moduleEnv = functionEnv.moduleEnv;
            final char GLOBAL = 'G';
            final char LOCAL = 'L';
            final char STACK = 'S';

            int iFirst = -1;
            char tFirst = 'X';
            for (int i = 0; i < names.length; i++) {
                VariableDecl decl = functionEnv.variables.get(names[i]);
                boolean isGlobal = false;
                if (decl == null) {
                    decl = moduleEnv.varDecl.get(names[i]);
                    isGlobal = true;
                }

                if (decl == null)
                    throw fail(ctx, "assignment", "Variable '" + names[i] + "' is not defined");

                if (!decl.mutable)
                    throw fail(ctx, "assignment", "Variable '" + names[i] + "' is not assignable");

                if (isGlobal || decl.inStack) {
                    if (iFirst >= 0)
                        throw fail(ctx, "assignment", "Can have at most one global or stack variable in chain assignment; found at least two, '" +
                                names[iFirst] + "' and '" + names[i] + "'");
                    iFirst = i;
                    tFirst = isGlobal? GLOBAL : STACK;
                }

                if (!decl.type.isValidRHS(type))
                    throw fail(ctx, "init", "Expression of type " + type + " cannot be assigned to variable '" + names[i] + "' of type " + decl.type);
            }

            Expression res = rhs;
            if (iFirst < 0) {
                iFirst = names.length - 1;
                tFirst = LOCAL;
            }
            for (int i = 0; i < names.length; i++) {
                if (i == iFirst)
                    continue;
                res = new TeeLocal(type.asNumType(), names[i], res);
            }

            Instruction ires = tFirst == LOCAL ? new SetLocal(names[iFirst], res) :
                               (tFirst == GLOBAL ? new SetGlobal(names[iFirst], res) :
                                       memory_store(type, new GetLocal(NumType.I32, names[iFirst]), res));

            return new Instruction[]{ires};
        }
    }

    static class DelayedLocalAccess extends Expression_Delayed {
        final String name;

        DelayedLocalAccess(String name) {
            this.name = name;
        }

        @Override
        public int complexity() {
            return 1;
        }

        @Override
        public String toString() {
            return "DelayedLocalAccess('" + name + "')";
        }

        @Override
        public Expression postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            VariableDecl decl = functionEnv.variables.get(name);
            assert decl != null;
            if (decl.inStack && !decl.isArray && !decl.type.is_struct()) {
                return memory_load(decl.type, new GetLocal(decl.type.asNumType(), decl.name));
            }
            else
                return new GetLocal(decl.type.is_struct()? NumType.I32 : decl.type.asNumType(), decl.name);
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
        String block_id;
        Instruction block_postfix;
        boolean need_block;
        BlockEnv () {
            block_id = null;
        }
        void markAsLoop(String block_id, Instruction block_postfix) {
            this.block_id = block_id;
            this.block_postfix = block_postfix;
            need_block = false;
        }
    }

    /*
    static class StructDeclaration extends CType {
        final String name;

        StructDeclaration(String name) {
            this.name = name;
        }

        @Override
        public NumType asNumType() {
            throw new RuntimeException("StructDeclaration cannot have aNumType");
        }

        @Override
        public int size() {
            throw new RuntimeException("StructDeclaration cannot have size");
        }

        @Override
        public String toString() {
            return "struct " + name;
        }

        @Override
        public boolean isValidRHS(CType rhs) {
            return rhs.is_struct(name);
        }

        @Override
        public boolean is_struct() {
            return true;
        }

        @Override
        public boolean is_struct(String name) {
            return this.name.equals(name);
        }
    }
    */

    @Override
    public ModuleEnv visitModule(c4waParser.ModuleContext ctx) {
        moduleEnv = new ModuleEnv(prop);

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
            else if (parseGlobalDecl instanceof StructDefinition) {
                StructDefinition def = (StructDefinition) parseGlobalDecl;
                moduleEnv.addStruct(def.name, new Struct(def.name, def.members));
            }
            else
                throw fail(g, "global item", "Unknown class " + parseGlobalDecl.getClass());
        }

        for (var oneFunc : functions) {
            functionEnv = oneFunc.func;

            functionEnv.setCode(((InstructionList) visit(oneFunc.code)).instructions);
            functionEnv.close();
            moduleEnv.addFunction(functionEnv);
        }

        if (functions.stream().noneMatch(f -> f.func.is_exported))
            System.out.println("WARNING: no extern functions, nothing will be exported");

        return moduleEnv;
    }

    @Override
    public StructDefinition visitStruct_definition(c4waParser.Struct_definitionContext ctx) {
        List<Struct.VarInput> members = new ArrayList<>();
        for(var d: ctx.struct_mult_members_decl()) {
            StructDefinition def = (StructDefinition) visit(d);
            members.addAll(Arrays.asList(def.members));
        }
        return new StructDefinition(ctx.ID().getText(), members.toArray(Struct.VarInput[]::new));
    }

    @Override
    public VariableDecl visitGlobal_decl_variable(c4waParser.Global_decl_variableContext ctx) {
        VariableDecl decl = (VariableDecl) visit(ctx.variable_decl());
        decl.exported = ctx.EXTERN() != null;
        decl.imported = ctx.STATIC() == null;
        decl.mutable = ctx.CONST() == null;

        if (ctx.expression() == null) {
            if (!decl.imported)
                throw fail(ctx, "global variable", "non-imported variable must be initialized");
        }
        else {
            if (decl.mutable && decl.imported)
                throw fail(ctx, "global variable","Imported global variable cannot be initialized, declare 'const' or 'static'");
            OneExpression rhs = (OneExpression) visit(ctx.expression());
            if (!(rhs.expression instanceof Const))
                throw fail(ctx, "global_variable", "RHS '" + ctx.expression().getText() + "' hasn't evaluated to a constant");
            decl.initialValue = new Const(decl.type.asNumType(), (Const) rhs.expression);
            // decl.initialValue = parseConstant(ctx, decl.type, ctx.CONSTANT().getText());
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

        functionEnv = new FunctionEnv(funcDecl.name, funcDecl.type, moduleEnv, ctx.EXTERN() != null);

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
            BlockEnv blockEnv = new BlockEnv(/* functionEnv.getMemOffset() */);
            var parent = ctx.getParent();
            if (parent instanceof c4waParser.Element_do_whileContext ||
                    parent instanceof c4waParser.Element_forContext)
                blockEnv.markAsLoop(functionEnv.getBlockId(), functionEnv.getBlockPostfix());

            List<Instruction> res = new ArrayList<>();

            blockStack.push(blockEnv);
            Partial parsedElem = visit(ctx.element());

            if (parsedElem instanceof OneInstruction)
                res.add(((OneInstruction) parsedElem).instruction);
            else if (!(parsedElem instanceof NoOp))
                throw new RuntimeException("Wrong type of parsedElem = " + parsedElem);
            blockStack.pop();

            return new InstructionList(res.toArray(Instruction[]::new), blockEnv.need_block);
        }
    }

    @Override
    public InstructionList visitComposite_block(c4waParser.Composite_blockContext ctx) {
        BlockEnv blockEnv = new BlockEnv(/* functionEnv.getMemOffset() */);

        // A hack obviously, but that's the best way I could find to meaningfully pass the knowledge
        // Basically we must know if this block is one associated with
        var parent = ctx.getParent();
        if (parent instanceof c4waParser.BlockContext)
            parent = parent.getParent();
        if (parent instanceof c4waParser.Element_do_whileContext ||
                parent instanceof c4waParser.Element_forContext)
            blockEnv.markAsLoop(functionEnv.getBlockId(), functionEnv.getBlockPostfix());

        blockStack.push(blockEnv);
        List<Instruction> res = new ArrayList<>();

        int idx = 0;
        for (var blockElem : ctx.element()) {
            idx ++;
            Partial parsedElem = visit(blockElem);
            if (parsedElem == null)
                throw fail(ctx, "block", "Instruction number " + idx + " was not parsed" +
                        ((idx == 1)?"":" (last parsed was '" + ctx.element(idx - 2).getText() + "')"));
            if (parsedElem instanceof OneInstruction)
                res.add(((OneInstruction) parsedElem).instruction);
            else if (parsedElem instanceof OneExpression) {
                OneExpression exp = (OneExpression) parsedElem;
                if (exp.expression instanceof CallExp)
                    res.add(new Drop(exp.expression));
                else
                    throw fail(blockElem, "block", "the only way to include expression as a statement is function call");
            }
            else if (!(parsedElem instanceof NoOp))
                throw new RuntimeException("Wrong type of parsedElem = " + parsedElem);
        }

        blockStack.pop();
        return new InstructionList(res.toArray(Instruction[]::new), blockEnv.need_block);
    }

    @Override
    public OneInstruction visitElement_do_while(c4waParser.Element_do_whileContext ctx) {
        OneExpression condition = (OneExpression) visit(ctx.expression());

        String block_id = functionEnv.pushBlock(null);
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        InstructionList body = (InstructionList) visit(ctx.block());
        functionEnv.popBlock();

        List<Instruction> body_elems = new ArrayList<>(Arrays.asList(body.instructions));

        if (condition.expression instanceof Const) {
            if (((Const) condition.expression).isTrue())
                body_elems.add(new Br(block_id_cont));
        }
        else
            body_elems.add(new BrIf(block_id_cont, condition.expression));
        if (body.need_block)
            return new OneInstruction(
                    new Block(block_id_break, new Instruction[]{
                            new Loop(block_id_cont, body_elems.toArray(Instruction[]::new))}));
        else
            return new OneInstruction(new Loop(block_id_cont, body_elems.toArray(Instruction[]::new)));
    }

    @Override
    public OneInstruction visitElement_for(c4waParser.Element_forContext ctx) {
        OneInstruction prestat = (OneInstruction) visit(ctx.pre);

        OneExpression condition = (OneExpression) visit(ctx.expression());
        OneInstruction poststat = (OneInstruction) visit(ctx.post);

        String block_id = functionEnv.pushBlock(poststat == null?null:poststat.instruction);
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        InstructionList body = (InstructionList) visit(ctx.block());
        functionEnv.popBlock();

        List<Instruction> loop_elems = new ArrayList<>();
        if (condition != null)
            loop_elems.add(new BrIf(block_id_break, condition.expression.Not(condition.type.asNumType())));
        loop_elems.addAll(Arrays.asList(body.instructions));
        if (poststat != null)
            loop_elems.add(poststat.instruction);
        loop_elems.add(new Br(block_id_cont));

        List<Instruction> block_elems = new ArrayList<>();
        if (prestat != null)
            block_elems.add(prestat.instruction);
        block_elems.add(new Loop(block_id_cont, loop_elems.toArray(Instruction[]::new)));

        return new OneInstruction(
                new Block(block_id_break, block_elems.toArray(Instruction[]::new)));
    }

    @Override
    public OneInstruction visitElement_break_continue_if(c4waParser.Element_break_continue_ifContext ctx) {
        boolean is_break = ctx.BREAK() != null;
        OneExpression condition = (OneExpression) visit(ctx.expression());

        return break_if(ctx, is_break, condition);
    }

    @Override
    public OneInstruction visitElement_break_continue(c4waParser.Element_break_continueContext ctx) {
        boolean is_break = ctx.BREAK() != null;
        return break_if(ctx, is_break, null);
    }

    private OneInstruction break_if(ParserRuleContext ctx, boolean is_break, OneExpression condition) {
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
                return new OneInstruction(new Br(ref));
            else
                return new OneInstruction(new BrIf(ref, condition.expression));
        }
        else {
            if (condition == null)
                return new OneInstruction(new DelayedList(List.of(blockEnv.block_postfix, new Br(ref))));
            else
                return new OneInstruction(
                        new IfThenElse(condition.expression,
                        new Instruction[]{blockEnv.block_postfix, new Br(ref)}, null));
        }
    }

    @Override
    public OneInstruction visitElement_if(c4waParser.Element_ifContext ctx) {
        OneExpression condition = (OneExpression) visit(ctx.expression());
        InstructionList thenList = (InstructionList) visit(ctx.block());

        return if_then_else(ctx, condition, thenList, null);
    }

    @Override
    public OneInstruction visitElement_if_else(c4waParser.Element_if_elseContext ctx) {
        OneExpression condition = (OneExpression) visit(ctx.expression());
        InstructionList thenList = (InstructionList) visit(ctx.block(0));
        InstructionList elseList = (InstructionList) visit(ctx.block(1));

        return if_then_else(ctx, condition, thenList, elseList);
    }

    @Override
    public OneExpression visitExpression_if_else(c4waParser.Expression_if_elseContext ctx) {
        OneExpression condition = (OneExpression) visit(ctx.expression(0));
        OneExpression thenExp = (OneExpression) visit(ctx.expression(1));
        OneExpression elseExp = (OneExpression) visit(ctx.expression(2));

        if (!thenExp.type.same(elseExp.type))
            throw fail(ctx, "ternary", "argument types '" + thenExp.type +
                    "' and '" + elseExp.type + "' are incompatible");

        return new OneExpression(new IfThenElseExp(condition.expression, thenExp.type.asNumType(), thenExp.expression,
                elseExp.expression), thenExp.type);
    }

    private OneInstruction if_then_else(ParserRuleContext ctx, OneExpression condition, InstructionList thenList,
                                        InstructionList elseList) {

        return new OneInstruction(new IfThenElse(condition.expression,
                thenList.instructions, (elseList == null)?null:elseList.instructions));
    }

    @Override
    public NoOp visitElement_empty(c4waParser.Element_emptyContext ctx) {
        return new NoOp();
    }

    @Override
    public Partial visitFunction_call(c4waParser.Function_callContext ctx) {
        String fname = ctx.ID().getText();

        ExpressionList args = (ctx.arg_list() == null)?(new ExpressionList()):(ExpressionList) visit(ctx.arg_list());

        if ("free".equals(fname)) {
            if (args.expressions.length != 1)
                throw fail(ctx, "function_call", "function '" + fname + "' expects 1 argument, received " +
                        args.expressions.length);
            if (!args.expressions[0].type.is_ptr())
                throw fail(ctx, "function_call", "Argument to `free' must be a pointer, received " + args.expressions[0].type);

            return new NoOp();
        }
        FunctionDecl decl = moduleEnv.funcDecl.get(fname);
        if (decl == null)
            throw fail(ctx, "function call", "Function '" + fname + "' not defined or declared");

        Expression[] call_args;
        Instruction func_call_void = null;
        Expression func_call_with_return = null;
        if (decl.anytype) {
            if (decl.returnType != null)
                throw fail(ctx, "function call", "'anytype' function with return value isn;t supported yet");

            List<Instruction> func_call_elms = new ArrayList<>();
            Expression getStack = new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME);
            functionEnv.markAsUsingStack();

            for (int idx = 0; idx < args.expressions.length; idx ++) {
                var arg = args.expressions[idx];

                if (arg.type.is_32() || arg.type.is_ptr()) {
                    NumType t64 = arg.type.is_int()|| arg.type.is_ptr()? NumType.I64 : NumType.F64;
                    func_call_elms.add(new Store(t64, getStack,
                            GenericCast.cast(arg.type.asNumType(), t64, arg.type.is_signed(), arg.expression)));
                }
                else
                    func_call_elms.add(new Store(arg.type.asNumType(), getStack, arg.expression));
                if (idx < args.expressions.length - 1)
                    func_call_elms.add(new SetGlobal(ModuleEnv.STACK_VAR_NAME, new Add(NumType.I32, getStack, new Const(8))));
                else
                    func_call_elms.add(new SetGlobal(ModuleEnv.STACK_VAR_NAME, new Sub(NumType.I32, getStack, new Const(8 * idx))));
            }

            call_args = new Expression[2];
            call_args[0] = getStack;
            call_args[1] = new Const(args.expressions.length);
            func_call_elms.add(new Call(fname, call_args));
            func_call_void = new DelayedList(func_call_elms);
        }
        else {
            if (decl.params.length != args.expressions.length)
                throw fail(ctx, "function call", "Function '" + fname +
                        "' expects " + decl.params.length + " arguments, provided " + args.expressions.length);

            call_args = new Expression[args.expressions.length];
            for(int idx = 0; idx < decl.params.length; idx ++) {
                if (!decl.params[idx].isValidRHS(args.expressions[idx].type))
                    throw fail(ctx, "function call", "Argument number " + (idx + 1) +
                            " of function '" + fname + "' expects type '" + decl.params[idx] +
                            "', received type '" + args.expressions[idx].type + "'");

                call_args[idx] = args.expressions[idx].expression;
            }
        }

        if ("memset".equals(fname))
            func_call_void = new MemoryFill(call_args[0], call_args[1], call_args[2]);
        else if ("memcpy".equals(fname))
            func_call_void = new MemoryCopy(call_args[0], call_args[1], call_args[2]);
        else if ("memgrow".equals(fname))
            func_call_void = new Drop (new MemoryGrow(call_args[0]));
        else if ("memsize".equals(fname))
            func_call_with_return = new MemorySize();
        else if (!decl.anytype) {
            if (decl.returnType == null)
                func_call_void = new Call(fname, call_args);
            else
                func_call_with_return = new CallExp(fname, decl.returnType.asNumType(), call_args);
        }

        if (decl.returnType == null) {
            assert func_call_void != null;
            return new OneInstruction(func_call_void);
        }
        else {
            assert func_call_with_return != null;
            return new OneExpression(func_call_with_return, decl.returnType);
        }
    }

    @Override
    public ExpressionList visitArg_list(c4waParser.Arg_listContext ctx) {
        return new ExpressionList(ctx.expression().stream().map(this::visit).toArray(OneExpression[]::new));
    }

    @Override
    public OneInstruction visitReturn_expression(c4waParser.Return_expressionContext ctx) {
        if (ctx.expression() == null)
            return new OneInstruction(new DelayedReturn());

        OneExpression expression = (OneExpression) visit(ctx.expression());
        if (functionEnv.returnType == null)
            throw fail(ctx, "return", "Function '" + functionEnv.name + "' doesn't return anything");

        if (!functionEnv.returnType.isValidRHS(expression.type))
            throw fail(ctx, "return", "Cannot return type '" + expression.type +
                    "' from a functiоn which is expected to return '" + functionEnv.returnType + "'");

        return new OneInstruction(new DelayedReturn(expression.expression));
    }

    @Override
    public OneInstruction visitVariable_init(c4waParser.Variable_initContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());
        OneExpression rhs = (OneExpression) visit(ctx.expression());

        if (!variableDecl.type.isValidRHS(rhs.type)) {
            if (rhs.expression instanceof Const && variableDecl.type.is_primitive())
                rhs = new OneExpression(new Const(variableDecl.type.asNumType(), (Const) rhs.expression), variableDecl.type);
            else
                throw fail(ctx, "init", "Expression of type " + rhs.type + " cannot be assigned to variable of type " + variableDecl.type);
        }

        if (functionEnv.variables.containsKey(variableDecl.name))
            throw fail(ctx, "init", "variable '" + variableDecl.name + "' already defined");
        functionEnv.registerVar(variableDecl.name, variableDecl.type, false);

        return new OneInstruction(new DelayedList(List.of(new DelayedLocalDefinition(variableDecl.name), new DelayedAssignment(ctx, new String[]{variableDecl.name}, rhs.expression, rhs.type))));
    }

    @Override
    public OneInstruction visitSimple_increment(c4waParser.Simple_incrementContext ctx) {
        String name = ctx.ID().getText();

        VariableDecl variableDecl = functionEnv.variables.get(name);
        CType type;
        boolean is_global = variableDecl == null;
        if (is_global) {
            VariableDecl decl = moduleEnv.varDecl.get(name);

            if (decl == null)
                throw fail(ctx, "increment", "Variable '" + name + "' is not defined");

            if (!decl.mutable)
                throw fail(ctx, "increment", "Global variable '" + name + "' is not mutable");

            type = decl.type;
        }
        else
            type = variableDecl.type;


        if(type.is_ptr())
            type = CType.INT;

        String op;
        OneExpression rhs;

        if (ctx.PLUSPLUS() != null) {
            op = "+";
            rhs = new OneExpression(new Const(type.asNumType(), 1), type);
        }
        else if (ctx.MINUSMINUS() != null) {
            op = "-";
            rhs = new OneExpression(new Const(type.asNumType(), 1), type);
        }
        else {
            op = ctx.op.getText().substring(0, ctx.op.getText().length() - 1);
            rhs = (OneExpression) visit(ctx.expression());
        }

        OneExpression binaryOp = binary_op(ctx, accessVariable(ctx, name), rhs, op);

        if (functionEnv.variables.containsKey(name))
            return new OneInstruction(new SetLocal(name, binaryOp.expression));
        else {
            VariableDecl decl = moduleEnv.varDecl.get(name);

            if (!decl.mutable)
                throw fail(ctx, "increment", "Global variable '" + name + "' is not mutable");

            return new OneInstruction(new SetGlobal(name, binaryOp.expression));
        }
    }

    @Override
    public OneInstruction visitSimple_assignment(c4waParser.Simple_assignmentContext ctx) {
        OneExpression rhs = (OneExpression) visit(ctx.expression());

        String[] names = ctx.ID().stream().map(ParseTree::getText).toArray(String[]::new);
        return new OneInstruction(new DelayedAssignment(ctx, names, rhs.expression, rhs.type));
    }

    @Override
    public OneInstruction visitComplex_increment(c4waParser.Complex_incrementContext ctx) {
        OneExpression lhs = (OneExpression) visit(ctx.lhs());

        String op;
        OneExpression rhs;
        CType type = lhs.type;

        if (ctx.PLUSPLUS() != null) {
            op = "+";
            rhs = new OneExpression(new Const(type.asNumType(), 1), type);
        } else if (ctx.MINUSMINUS() != null) {
            op = "-";
            rhs = new OneExpression(new Const(type.asNumType(), 1), type);
        } else {
            op = ctx.op.getText().substring(0, ctx.op.getText().length() - 1);
            rhs = (OneExpression) visit(ctx.expression());
        }

        if (lhs.expression.complexity() <= 3) {
            OneExpression binaryOp = binary_op(ctx, new OneExpression(memory_load(type,lhs.expression), type), rhs, op);
            return new OneInstruction(memory_store(lhs, binaryOp));

        }
        else {
            String tempVar = functionEnv.temporaryVar(NumType.I32);

            OneExpression new_lhs = new OneExpression(new GetLocal(type.asNumType(), tempVar), type);
            OneExpression binaryOp = binary_op(ctx, new OneExpression(memory_load(type, new_lhs.expression), type), rhs, op);

            return new OneInstruction(new DelayedList(List.of(new SetLocal(tempVar, lhs.expression), memory_store(new_lhs, binaryOp))));
        }
    }

    @Override
    public OneInstruction visitComplex_assignment(c4waParser.Complex_assignmentContext ctx) {
        OneExpression lhs = (OneExpression) visit(ctx.lhs());
        OneExpression rhs = (OneExpression) visit(ctx.expression());

        if (!lhs.type.isValidRHS(rhs.type)) {
            if (rhs.expression instanceof Const && lhs.type.is_primitive())
                rhs = new OneExpression(new Const(lhs.type.asNumType(), (Const) rhs.expression), lhs.type);
            else
                throw fail(ctx, "assign", "Expression of type " + rhs.type + " cannot be assigned to '" + lhs.type + "'");
        }

        return new OneInstruction(memory_store(lhs, rhs));
    }

    static private Store memory_store(OneExpression lhs, OneExpression rhs) {
        return memory_store(lhs.type, lhs.expression, rhs.expression);
    }

    static private Store memory_store(CType type, Expression lhs, Expression rhs) {
        if (type.same(CType.CHAR))
            return new Store(type.asNumType(), 8, lhs, rhs);
        else if (type.same(CType.SHORT))
            return new Store(type.asNumType(), 16, lhs, rhs);
        else
            return new Store(type.asNumType(), lhs, rhs);
    }

    static private Load memory_load(CType type, Expression i) {
        if (type.size() == 1)
            return new Load(type.asNumType(), 8, type.is_signed(), i);
        else if (type.size() == 2)
            return new Load(type.asNumType(), 16, type.is_signed(), i);
        else
            return new Load(type.asNumType(), i);
    }


    @Override
    public OneInstruction visitMult_variable_decl(c4waParser.Mult_variable_declContext ctx) {
        List<Instruction> res = new ArrayList<>();
        CType o_type = (CType) visit(ctx.primitive());

        for (var v : ctx.local_variable()) {
            LocalVariable localVar = (LocalVariable) visit(v);

            CType type = o_type;
            for (int i = 0; i < localVar.ref_level; i++)
                type = type.make_pointer_to();

            functionEnv.registerVar(localVar.name, localVar.size == null? type: type.make_pointer_to(), false);

            if (type.is_struct() || localVar.size != null) {
                functionEnv.variables.get(localVar.name).mutable = false;
                functionEnv.variables.get(localVar.name).inStack = true;
                functionEnv.variables.get(localVar.name).isArray = localVar.size != null;

                GetGlobal stack = new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME);
                res.add(new SetLocal(localVar.name, stack));
                res.add(new SetGlobal(ModuleEnv.STACK_VAR_NAME, new Add(NumType.I32, stack,
                        localVar.size == null
                                ? new Const(type.size())
                                : new Mul(NumType.I32, localVar.size, new Const(type.size())))));

                functionEnv.markAsUsingStack();
            }
            else
                res.add(new DelayedLocalDefinition(localVar.name));
        }

        return new OneInstruction(new DelayedList(res));
    }

    @Override
    public StructDefinition visitStruct_mult_members_decl(c4waParser.Struct_mult_members_declContext ctx) {
        CType o_type = (CType) visit(ctx.primitive());
        List<Struct.VarInput> members = new ArrayList<>();

        for (var v : ctx.struct_member_decl()) {
            StructMember memb = (StructMember) visit(v);
            CType type = o_type;
            for (int i = 0; i < memb.ref_level; i++)
                type = type.make_pointer_to();

            if (type instanceof StructDecl)
                throw fail(ctx, "struct", "Can't use undefined structure '" + ((StructDecl) type).name +
                        "' as struct member; did you mean to use a pointer instead?");

            members.add(new Struct.VarInput(memb.name, type, memb.size));
        }

        return new StructDefinition(members.toArray(Struct.VarInput[]::new));
    }

    @Override
    public LocalVariable visitLocal_variable(c4waParser.Local_variableContext ctx) {
        OneExpression size = null;
        if (ctx.expression() != null) {
            size = (OneExpression) visit(ctx.expression());
            if (!size.type.is_i32())
                throw fail(ctx, "local variable", "Array size must be INT, not '" + size.type + "'");
        }
        return new LocalVariable(ctx.ID().getText(), ctx.MULT().size(), size == null? null: size.expression);
    }

    @Override
    public StructMember visitStruct_member_decl(c4waParser.Struct_member_declContext ctx) {
        Integer size = null;
        if (ctx.expression() != null) {
            OneExpression exp = (OneExpression) visit(ctx.expression());
            if (!(exp.expression instanceof Const))
                throw fail(ctx, "struct", "Array size in structure must be constant");
            Const c = (Const)exp.expression;
            if (!c.is_int())
                throw fail(ctx, "struct", "Array size in structure must be integer");

            size = (int) c.longValue;
        }
        return new StructMember(ctx.ID().getText(), ctx.MULT().size(), size);
    }

    @Override
    public OneExpression visitExpression_sizeof_type(c4waParser.Expression_sizeof_typeContext ctx) {
        CType type = (CType) visit(ctx.variable_type());
        return new OneExpression(new Const(type.size()), CType.INT);
    }

    @Override
    public OneExpression visitExpression_sizeof_exp(c4waParser.Expression_sizeof_expContext ctx) {
        OneExpression e = (OneExpression) visit(ctx.expression());

        return new OneExpression(new Const(e.type.size()), CType.INT);
    }

    @Override
    public OneExpression visitLhs_dereference(c4waParser.Lhs_dereferenceContext ctx) {
        OneExpression ptr = (OneExpression) visit(ctx.expression());

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "dereference", "trying to dereference '" + ptr.type + "' which is not a reference");

        return new OneExpression(ptr.expression, type);
    }

    @Override
    public OneExpression visitExpression_addr_var(c4waParser.Expression_addr_varContext ctx) {
        String name = ctx.ID().getText();
        VariableDecl decl = functionEnv.variables.get(name);

        if (decl == null)
            throw fail(ctx, "address of variable", "'" + name + "' is not a local variable");

        if (decl.isArray)
            throw fail(ctx, "address of variable", "'" + name + "' is array, cannot take address of an array");

        decl.inStack = true;

        return new OneExpression(new GetLocal(NumType.I32, name), decl.type.make_pointer_to());
    }

    @Override
    public OneExpression visitExpression_addr_lhs(c4waParser.Expression_addr_lhsContext ctx) {
        OneExpression exp = (OneExpression) visit(ctx.lhs());

        return new OneExpression(exp.expression, exp.type.make_pointer_to());
    }

    @Override
    public OneExpression visitExpression_struct_member(c4waParser.Expression_struct_memberContext ctx) {
        OneExpression ptr = (OneExpression) visit(ctx.expression());

        if (ptr.type.deref() == null)
            throw fail(ctx, "struct_member", "'" + ptr.type + "' is not a pointer");
        if (!ptr.type.deref().is_struct())
            throw fail(ctx, "struct_member", "'" + ptr.type.deref() + "' is not a defined struct");

        return rhs_struct_member(ctx, (Struct) ptr.type.deref(), ctx.ID().getText(), ptr.expression);
    }

    @Override
    public OneExpression visitExpression_struct_member_dot(c4waParser.Expression_struct_member_dotContext ctx) {
        OneExpression str = (OneExpression) visit(ctx.expression());

        if (!str.type.is_struct())
            throw fail(ctx, "struct_member_dor", "'" + str.type + "' is not a defined struct");

        return rhs_struct_member(ctx, (Struct) str.type, ctx.ID().getText(), str.expression);
    }

    @Override
    public OneExpression visitLhs_struct_member(c4waParser.Lhs_struct_memberContext ctx) {
        OneExpression ptr = (OneExpression) visit(ctx.expression());

        if (ptr.type.deref() == null)
            throw fail(ctx, "struct_member", "'" + ptr.type + "' is not a pointer");

        if (!ptr.type.deref().is_struct())
            throw fail(ctx, "struct_member", "'" + ptr.type.deref() + "' is not a defined struct");

        return lhs_struct_member(ctx, (Struct) ptr.type.deref(), ctx.ID().getText(), ptr.expression);
    }

    @Override
    public OneExpression visitLhs_struct_member_dot(c4waParser.Lhs_struct_member_dotContext ctx) {
        OneExpression str = (OneExpression) visit(ctx.expression());

        if (!str.type.is_struct())
            throw fail(ctx, "struct_member_dor", "'" + str.type + "' is not a defined struct");

        return lhs_struct_member (ctx, (Struct) str.type, ctx.ID().getText(), str.expression);
    }

    private OneExpression lhs_struct_member(ParserRuleContext ctx, Struct c_struct, String mem, Expression expression) {
        Struct.Var mInfo = c_struct.m.get(mem);

        if (mInfo == null)
            throw fail(ctx, "struct_member", "No member '" + mem + "' in " + c_struct);

        CType type = mInfo.type;

        if (mInfo.size != null)
            throw fail(ctx, "struct_member", "Array member '" + mem + "' cannot be assigned to");

        Expression memAddress = new Add(NumType.I32, expression, new Const(mInfo.offset));

        return new OneExpression(memAddress, type);
    }

    private OneExpression rhs_struct_member(ParserRuleContext ctx, Struct c_struct, String mem, Expression expression) {
        Struct.Var mInfo = c_struct.m.get(mem);

        if (mInfo == null)
            throw fail(ctx, "struct_member", "No member '" + mem + "' in " + c_struct);

        CType type = mInfo.type;

        Expression memAddress = new Add(NumType.I32, expression, new Const(mInfo.offset));

        if (mInfo.size != null || type.is_struct())
            return new OneExpression(memAddress, type);

        return new OneExpression(memory_load(type, memAddress), type);
    }

    @Override
    public OneExpression visitLhs_index(c4waParser.Lhs_indexContext ctx) {
        OneExpression ptr = (OneExpression) visit(ctx.ptr);
        OneExpression idx = (OneExpression) visit(ctx.idx);

        if (!idx.type.same(CType.INT))
            throw fail(ctx, "index", "index must be INT, got '" + idx.type + "'");

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "index", "trying to dereference '" + ptr.type + "' which is not a reference");

        if (type.size() == 1)
            return new OneExpression(new Add(NumType.I32, ptr.expression, idx.expression), type);
        else
            return new OneExpression(new Add(NumType.I32, ptr.expression,
                    new Mul(NumType.I32, idx.expression, new Const(type.size()))), type);
    }

    @Override
    public OneExpression visitExpression_index(c4waParser.Expression_indexContext ctx) {
        OneExpression ptr = (OneExpression) visit(ctx.ptr);
        OneExpression idx = (OneExpression) visit(ctx.idx);

        if (!idx.type.same(CType.INT))
            throw fail(ctx, "index", "index must be INT, got '" + idx.type + "'");

        CType type = ptr.type.deref();

        if (type == null)
            throw fail(ctx, "index", "trying to dereference '" + ptr.type + "' which is not a reference");

        Expression memAddress = (type.size() == 1)
                ? new Add(NumType.I32, ptr.expression, idx.expression)
                : new Add(NumType.I32, ptr.expression, new Mul(NumType.I32, idx.expression, new Const(type.size())));

        return new OneExpression(memory_load(type, memAddress), type);
    }

    @Override
    public OneExpression visitExpression_binary_cmp(c4waParser.Expression_binary_cmpContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_add(c4waParser.Expression_binary_addContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_shift(c4waParser.Expression_binary_shiftContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_mult(c4waParser.Expression_binary_multContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_or(c4waParser.Expression_binary_orContext ctx) {
        List<c4waParser.ExpressionContext> components = new ArrayList<>();

        do {
            components.add(ctx.left);
            c4waParser.ExpressionContext vctx = ctx.right;
            if (vctx instanceof c4waParser.Expression_binary_orContext)
                ctx = (c4waParser.Expression_binary_orContext) vctx;
            else {
                components.add(vctx);
                break;
            }
        }
        while(true);

        return and_or_chain(components, false);
    }

    @Override
    public OneExpression visitExpression_binary_and(c4waParser.Expression_binary_andContext ctx) {
        List<c4waParser.ExpressionContext> components = new ArrayList<>();

        do {
            components.add(ctx.left);
            c4waParser.ExpressionContext vctx = ctx.right;
            if (vctx instanceof c4waParser.Expression_binary_andContext)
                ctx = (c4waParser.Expression_binary_andContext) vctx;
            else {
                components.add(vctx);
                break;
            }
        }
        while (true);

        return and_or_chain(components, true);
    }

    private OneExpression and_or_chain(List<c4waParser.ExpressionContext> ctxList, boolean is_and) {
        OneExpression[] exp = new OneExpression[ctxList.size()];
        Expression[] condition = new Expression[exp.length];
        int i = -1;
        for (var ctx: ctxList) {
            i ++;
            exp[i] = (OneExpression) visit(ctx);
            if (!(exp[i].type.is_int() || exp[i].type.is_ptr()))
                throw fail(ctx, "AND", "Type '" + exp[i].type + "' is invalid for boolean operations");

            condition[i] = is_and ?
                            exp[i].expression.Not(exp[i].type.asNumType())
                     :( exp[i].type.is_i64() ?
                            GenericCast.cast(exp[i].type.asNumType(), NumType.I32, false, exp[i].expression)
                    :       exp[i].expression);
        }

        if (exp.length == 2)
            return new OneExpression(new IfThenElseExp(condition[0], NumType.I32,
                    new Const(is_and? 0 : 1),
                    new Cmp(exp[1].type.asNumType(), false, exp[1].expression,
                                    new Const(exp[1].type.asNumType(), 0))), CType.INT);

        String block_id = functionEnv.pushBlock(null);
        String block_id_break = block_id + BREAK_SUFFIX;

        Instruction[] elm = new Instruction[exp.length];

        for (i = 0; i < exp.length; i ++)
            elm[i] = new Drop(new BrIfExp(block_id_break, NumType.I32, condition[i], new Const(is_and ? 0 : 1)));

        functionEnv.popBlock();

        return new OneExpression(new BlockExp(block_id_break, NumType.I32, elm, new Const(is_and ? 1 : 0)), CType.INT);
    }

    @Override
    public OneExpression visitExpression_binary_bwxor(c4waParser.Expression_binary_bwxorContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_bwand(c4waParser.Expression_binary_bwandContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_bwor(c4waParser.Expression_binary_bworContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    private OneExpression binary_op (ParserRuleContext ctx, OneExpression arg1, OneExpression arg2, String op) {
        if (arg1 == null)
            throw fail(ctx, "Expression", "1-st arg not parsed");
        if (arg2 == null)
            throw fail(ctx, "Expression", "2-nd arg not parsed");

        if (arg1.type.is_i64() && arg2.type.is_i32() && arg2.expression instanceof Const) {
            arg2 = new OneExpression(new Const((((Const) arg2.expression).longValue)), arg1.type);
        }
        else if (arg2.type.is_i64() && arg1.type.is_i32() && arg1.expression instanceof Const) {
            arg1 = new OneExpression(new Const((((Const) arg1.expression).longValue)), arg2.type);
        }
        else if (arg1.type.is_f64() && arg2.type.is_f32() && arg2.expression instanceof Const) {
            arg2 = new OneExpression(new Const((((Const) arg2.expression).doubleValue)), arg1.type);
        }
        else if (arg2.type.is_f64() && arg1.type.is_f32() && arg1.expression instanceof Const) {
            arg1 = new OneExpression(new Const((((Const) arg1.expression).doubleValue)), arg2.type);
        }

        NumType numType;
        CType resType;

        if ("+".equals(op) && arg2.type.same(CType.INT) && arg1.type.deref() != null) {
            CType type = arg1.type.deref();
            return new OneExpression((type.size() == 1)
                    ? new Add(NumType.I32, arg1.expression, arg2.expression)
                    : new Add(NumType.I32, arg1.expression, new Mul(NumType.I32, arg2.expression, new Const(type.size()))),
                    arg1.type);
        }
        if ("-".equals(op) && arg2.type.same(CType.INT) && arg1.type.deref() != null) {
            CType type = arg1.type.deref();
            return new OneExpression((type.size() == 1)
                    ? new Sub(NumType.I32, arg1.expression, arg2.expression)
                    : new Sub(NumType.I32, arg1.expression, new Mul(NumType.I32, arg2.expression, new Const(type.size()))),
                    arg1.type);
        }
        else if ("+".equals(op) && arg1.type.same(CType.INT) && arg2.type.deref() != null) {
            CType type = arg2.type.deref();
            return new OneExpression((type.size() == 1)
                    ? new Add(NumType.I32, arg2.expression, arg1.expression)
                    : new Add(NumType.I32, arg2.expression, new Mul(NumType.I32, arg1.expression, new Const(type.size()))),
                    arg2.type);
        }
        else if ("-".equals(op) && arg1.type.is_ptr() && arg2.type.is_ptr() && arg1.type.same(arg2.type))
            return new OneExpression(arg1.type.deref().size() == 1
                    ? new Sub(NumType.I32, arg1.expression, arg2.expression)
                    : new Div(NumType.I32, true, new Sub(NumType.I32, arg1.expression, arg2.expression), new Const(arg1.type.deref().size())),
            CType.INT);
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
        } else if (arg1.type.is_ptr() && arg2.type.is_ptr() && arg1.type.same(arg2.type) && List.of( "==", "!=").contains(op)) {
            numType = NumType.I32;
            resType = CType.INT;
        }
        else
            throw fail(ctx, "binary operation '" + op + "'", "Types " + arg1.type + " and " + arg2.type +
                    " are incompatible");

        if (arg1.type.is_signed() != arg2.type.is_signed())
            throw fail(ctx, "binary operation '" + op + "'", "cannot combined signed and unsigned types");

        resType = resType.make_signed(arg1.type.is_signed());

        Expression res;
        if ("+".equals(op))
            res = new Add(numType, arg1.expression, arg2.expression);
        else if ("-".equals(op))
            res = new Sub(numType, arg1.expression, arg2.expression);
        else if ("*".equals(op))
            res = new Mul(numType, arg1.expression, arg2.expression);
        else if ("/".equals(op))
            res = new Div(numType, arg1.type.is_signed(), arg1.expression, arg2.expression);
        else if ("%".equals(op))
            res = new Rem(numType, arg1.type.is_signed(), arg1.expression, arg2.expression);
        else if (List.of("<", "<=", ">", ">=").contains(op))
            res = new Cmp(numType, op.charAt(0) == '<', op.length() == 2, arg1.type.is_signed(),
                    arg1.expression, arg2.expression);
        else if (op.equals("==") && arg1.expression instanceof Const && ((Const)arg1.expression).longValue == 0 && arg1.type.is_int())
            res = new Eqz(numType, arg2.expression);
        else if (op.equals("==") && arg2.expression instanceof Const && ((Const)arg2.expression).longValue == 0 && arg1.type.is_int())
            res = new Eqz(numType, arg1.expression);
        else if (List.of("==", "!=").contains(op))
            res = new Cmp(numType, op.charAt(0) == '=', arg1.expression, arg2.expression);
        else if ("&&".equals(op) || "&".equals(op))
            res = new And(numType, arg1.expression, arg2.expression);
        else if ("||".equals(op) || "|".equals(op))
            res = new Or(numType, arg1.expression, arg2.expression);
        else if ("^".equals(op))
            res = new Xor(numType, arg1.expression, arg2.expression);
        else if (List.of("<<", ">>").contains(op))
            res = new Shift(numType, op.charAt(0) == '<', arg1.type.is_signed(), arg1.expression, arg2.expression);
        else
            throw fail(ctx, "binary operation", "Instruction '" + op + "' not recognized");

        return new OneExpression(res, resType);
    }

    @Override
    public OneExpression visitExpression_unary_op(c4waParser.Expression_unary_opContext ctx) {
        String op = ctx.op.getText();
        OneExpression exp = (OneExpression) visit(ctx.expression());

        if (op.equals("-")) {
            if (exp.type.is_int())
                return new OneExpression(new Sub(exp.type.asNumType(), new Const(0), exp.expression), exp.type);
            else
                return new OneExpression(new Neg(exp.type.asNumType(), exp.expression), exp.type);
        }
        else if (op.equals("!")) {
            return new OneExpression(exp.expression.Not(exp.type.asNumType()), exp.type);
        }
        else if (op.equals("*")) {
            CType type = exp.type.deref();
            if (type == null)
                throw fail(ctx, "unary_op", "Trying to dereference '" + exp.type + "', must be a pointer");

            return new OneExpression(memory_load(type, exp.expression), type);
        }
        else if (op.equals("~")) {
            return new OneExpression(new Xor(exp.type.asNumType(), exp.expression, new Const(exp.type.asNumType(), -1)), exp.type);
        } else
            throw fail(ctx, "unary_op", "Operation '" + op + "' not recognized");
    }

    @Override
    public OneExpression visitExpression_alloc(c4waParser.Expression_allocContext ctx) {
        OneExpression memptr = (OneExpression) visit(ctx.memptr);
        CType type = (CType) visit(ctx.variable_type());

        if (!memptr.type.is_i32())
            throw fail(ctx, "alloc", "'" + memptr.type + "' won't work for alloc, must have int");

        int memOffset = moduleEnv.STACK_SIZE + moduleEnv.DATA_SIZE;
        if (memptr.expression instanceof Const)
            return new OneExpression(new Const(memOffset + (int)((Const)memptr.expression).longValue), type.make_pointer_to());
        else
            return new OneExpression(new Add(NumType.I32, memptr.expression, new Const(memOffset)), type.make_pointer_to());
    }

    @Override
    public OneExpression visitExpression_cast(c4waParser.Expression_castContext ctx) {
        OneExpression exp = (OneExpression) visit(ctx.expression());
        CType castToType = (CType) visit(ctx.variable_type());

        boolean signed;
        if (exp.type.is_int())
            signed = exp.type.is_signed();
        else if (castToType.is_int())
            signed = castToType.is_signed();
        else
            signed = true;

        return new OneExpression(GenericCast.cast(exp.type.asNumType(), castToType.asNumType(), signed, exp.expression), castToType);
    }

    @Override
    public OneExpression visitExpression_variable(c4waParser.Expression_variableContext ctx) {
        return accessVariable(ctx, ctx.ID().getText());
    }

    private OneExpression accessVariable(ParserRuleContext ctx, String name) {
        VariableDecl decl = functionEnv.variables.get(name);

        if (decl != null)
//            return new OneExpression(new GetLocal(decl.type.asNumType(), name), decl.type);
            return new OneExpression(new DelayedLocalAccess(name), decl.type);

        decl = moduleEnv.varDecl.get(name);

        if (decl != null)
            return new OneExpression(new GetGlobal(decl.type.asNumType(), name), decl.type);

        throw fail(ctx, "variable", "'" + name + "' not defined");
    }

    @Override
    public OneExpression visitExpression_const(c4waParser.Expression_constContext ctx) {
        return parseConstant(ctx, ctx.CONSTANT().getText());
    }

    @Override
    public OneExpression visitExpression_string(c4waParser.Expression_stringContext ctx) {
        StringBuilder b = new StringBuilder();
        for (var s : ctx.STRING())
            b.append(unescape(s.getText()));

        //String str = unescape(ctx.STRING().getText());
        return new OneExpression(new Const(moduleEnv.addString(b.toString())), CType.CHAR.make_pointer_to());
    }

    @Override
    public OneExpression visitExpression_character(c4waParser.Expression_characterContext ctx) {
        String s = ctx.CHARACTER().getText();
        Integer val = unescapeChar(s);
        if (val == null)
            throw fail(ctx, "character", "Invalid character definition " + s);
        return new OneExpression(new Const(val), CType.CHAR);
    }

    @Override
    public CType visitInteger_primitive(c4waParser.Integer_primitiveContext ctx) {
        boolean u = ctx.UNSIGNED() != null;
        if (ctx.CHAR() != null)
            return u? CType.UNSIGNED_CHAR: CType.CHAR;
        else if (ctx.SHORT() != null)
            return u? CType.UNSIGNED_SHORT: CType.SHORT;
        else if (ctx.INT() != null)
            return u? CType.UNSIGNED_INT : CType.INT;
        else if (ctx.LONG() != null)
            return u? CType.UNSIGNED_LONG : CType.LONG;
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
            return new StructDecl(name);

        return c_struct;
    }

    private OneExpression parseConstant(ParserRuleContext ctx, String textOfConstant) {
        try {
            return new OneExpression(new Const(Integer.parseInt(textOfConstant)), CType.INT);
        } catch (NumberFormatException ignored) {
        }

        try {
            return new OneExpression(new Const(Long.parseLong(textOfConstant)), CType.LONG);
        } catch (NumberFormatException ignored) {
        }

        try {
            return new OneExpression(new Const(Double.parseDouble(textOfConstant)), CType.DOUBLE);
        } catch (NumberFormatException ignored) {
        }

        throw fail(ctx, "const", "'" + textOfConstant + "' cannot be parsed");
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

    static Integer unescapeChar(String str) {
        byte[] bytes = str.substring(1, str.length() - 1).getBytes();

        if (bytes.length == 1)
            return (int)bytes[0];
        else if (bytes[0] != '\\')
            return null;
        else if (bytes[1] == 'a')
            return 0x07;
        else if (bytes[1] == 'b')
            return 0x08;
        else if (bytes[1] == 'e')
            return 0x1B;
        else if (bytes[1] == 'f')
            return 0x0C;
        else if (bytes[1] == 'n')
            return 0x0A;
        else if (bytes[1] == 'r')
            return 0x0D;
        else if (bytes[1] == 't')
            return 0x09;
        else if (bytes[1] == 'v')
            return 0x0B;
        else if (bytes[1] == '\\')
            return 0x5C;
        else if (bytes[1] == '\'')
            return 0x27;
        else if (bytes[1] == '"')
            return 0x22;
        else if (bytes[1] == '?')
            return 0x3F;
        else if ('0' <= bytes[1] && bytes[1] <= '7' && bytes.length <= 4) {
            int res = bytes.length == 4 ? (bytes[1] - '0')*64 + (bytes[2] - '0') * 8 + (bytes[3] - '0')
                    : (bytes.length == 3 ? (bytes[1] - '0')*8 + (bytes[2] - '0') : bytes[1] - '0');
            if (res >= 256)
                return null;
            return res;
        }
        else if (bytes[1] == 'x' && bytes.length == 4) {
            return Integer.parseInt(Character.toString(bytes[2]) + Character.toString(bytes[3]), 16);
        }
        else
            return null;
    }

    static String unescape(String str) {
        // we process \" and \\, but the rest of escape sequences are passed as-is
        // so "\n" is just a regular two-char sequence, <\> and <n>.
        StringBuilder b = new StringBuilder();
        int N = str.length();
        for (int i = 1; i < N - 1; i ++) {
            if (str.charAt(i) == '\\' && "\\\"".contains(String.valueOf(str.charAt(i+1))))
                i ++;

            b.append(str.charAt(i));
        }
        return b.toString();
    }

    static private RuntimeException fail(ParserRuleContext ctx, String desc, String error) {
        if (print_stack_trace_on_errors)
            return new RuntimeException("[" + ctx.start.getLine() + ":" +
                    ctx.start.getCharPositionInLine() + "] " + desc + " " + ctx.getText() + " : " + error);
        else
            return new SyntaxError(ctx.start.getLine(), ctx.stop.getLine(), ctx.start.getCharPositionInLine(), ctx.stop.getCharPositionInLine(),
                    error + " (" + desc + ")");
    }

}
