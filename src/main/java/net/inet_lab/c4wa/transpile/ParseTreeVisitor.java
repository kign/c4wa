package net.inet_lab.c4wa.transpile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jetbrains.annotations.NotNull;

import net.inet_lab.c4wa.autogen.cparser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.cparser.c4waParser;
import net.inet_lab.c4wa.wat.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class ParseTreeVisitor extends c4waBaseVisitor<Partial> {
    private FunctionEnv functionEnv;
    final private ModuleEnv moduleEnv;
    final private Deque<BlockEnv> blockStack;

    final private static String CONT_SUFFIX = "_continue";
    final private static String BREAK_SUFFIX = "_break";
    static final private boolean print_stack_trace_on_errors = false;

    public ParseTreeVisitor(ModuleEnv moduleEnv) {
        blockStack = new ArrayDeque<>();
        this.moduleEnv = moduleEnv;
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
        InstructionList(Instruction[] instructions) {
            this.instructions = instructions;
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
        final String varId;
        final Expression sizeExp;
        DelayedLocalDefinition(String varId, Expression sizeExp) {
            this.varId = varId;
            this.sizeExp = sizeExp;
        }

        @Override
        public String toString() {
            return "DelayedLocalDefinition('" + varId + "')";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            VariableDecl decl = functionEnv.getVariableDecl(varId);
            assert decl != null;
            if (decl.inStack) {
                CType type = decl.type;
                GetGlobal stack = new GetGlobal(NumType.I32, ModuleEnv.STACK_VAR_NAME);
                String watName = functionEnv.getVariableWAT(varId);

                if (type.is_struct() || decl.isArray)
                    return new Instruction[] {
                            new SetLocal(watName, stack),
                            new SetGlobal(ModuleEnv.STACK_VAR_NAME, new Add(NumType.I32, stack,
                                    decl.isArray
                                            ? new Mul(NumType.I32, sizeExp, new Const(type.deref().size()))
                                            : new Const(type.size())))};
                else
                    return new Instruction[]{
                            new SetLocal(watName, stack),
                            new SetGlobal(ModuleEnv.STACK_VAR_NAME,
                                    new Add(NumType.I32, stack, new Const(type.size())))};
            }
            else
                return new Instruction[0];
        }
    }

    static class DelayedAssignment extends Instruction_Delayed {
        final String[] varId_a;
        final Expression rhs;
        final CType type;
        final ParserRuleContext ctx;
        final boolean init;

        DelayedAssignment(ParserRuleContext ctx, boolean init, String[] varId_a, Expression rhs, CType type) {
            this.ctx = ctx;
            this.varId_a = varId_a;
            this.rhs = rhs;
            this.type = type;
            this.init = init;
        }

        @Override
        public String toString() {
            return "DelayedAssignment(" + String.join(",", varId_a)  + ")";
        }

        @Override
        public Instruction[] postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            String[] watNames = Arrays.stream(varId_a).map(functionEnv::getVariableWAT).toArray(String[]::new);

            /* This is a stupid optimization; we are trying to take advantage of the fact that in WASM
             * all locals are initialized to 0, so when defining new var like this "int x = 0" initialization
             * could be dropped.
             *
             * Of course, it only works if
             *    (a) this is initialization, not assignment;
             *    (b) this is not a stack variable;
             *    (c) WAT variable name isn't a reuse;
             *    (d) We are not inside a loop.
             */
            if (init && rhs instanceof Const) {
                Const c = (Const) rhs;
                if (c.is_int() && c.longValue == 0 || !c.is_int() && c.doubleValue == 0.0) {
                    // WASM local variables are initialized to 0
                    if (varId_a.length != 1)
                        throw new RuntimeException("names.length = " + varId_a.length);

                    VariableDecl decl = functionEnv.getVariableDecl(varId_a[0]);
                    if (decl == null)
                        throw new RuntimeException("Missing variable " + varId_a[0]);

                    if (!decl.inStack && !functionEnv.isWATNameReused(watNames[0]))
                        return new Instruction[0];
                }
            }

            ModuleEnv moduleEnv = functionEnv.moduleEnv;
            final char GLOBAL = 'G';
            final char LOCAL = 'L';
            final char STACK = 'S';

            int iFirst = -1;
            char tFirst = 'X';
            for (int i = 0; i < varId_a.length; i++) {
                VariableDecl decl = functionEnv.getVariableDecl(varId_a[i]);
                boolean isGlobal = false;
                if (decl == null) {
                    decl = moduleEnv.varDecl.get(varId_a[i]);
                    isGlobal = true;
                }

                if (decl == null)
                    throw fail(ctx, "assignment", "Variable '" + varId_a[i] + "' is not defined");

                if (!decl.mutable && !init)
                    throw fail(ctx, "assignment", "Variable '" + varId_a[i] + "' is not assignable");

                if (isGlobal || decl.inStack) {
                    if (iFirst >= 0)
                        throw fail(ctx, "assignment", "Can have at most one global or stack variable in chain assignment; found at least two, '" +
                                varId_a[iFirst] + "' and '" + varId_a[i] + "'");
                    iFirst = i;
                    tFirst = isGlobal? GLOBAL : STACK;
                }

                if (!decl.type.isValidRHS(type))
                    throw fail(ctx, "init", "Expression of type " + type + " cannot be assigned to variable '" + varId_a[i] + "' of type " + decl.type);
            }

            Expression res = rhs;
            if (iFirst < 0) {
                iFirst = varId_a.length - 1;
                tFirst = LOCAL;
            }
            for (int i = 0; i < varId_a.length; i++) {
                if (i == iFirst)
                    continue;
                res = new TeeLocal(type.asNumType(), watNames[i], res);
            }

            Instruction ires = tFirst == LOCAL ? new SetLocal(watNames[iFirst], res) :
                               (tFirst == GLOBAL ? new SetGlobal(watNames[iFirst], res) :
                                       memory_store(type, new GetLocal(NumType.I32, watNames[iFirst]), res));

            return new Instruction[]{ires};
        }
    }

    static class DelayedLocalAccess extends Expression_Delayed {
        final String varId;
        final boolean address;

        DelayedLocalAccess(String varId, boolean address) {
            this.varId = varId;
            this.address = address;
        }

        @Override
        public int complexity() {
            return 1;
        }

        @Override
        public String toString() {
            return "DelayedLocalAccess('" + varId + "')";
        }

        @Override
        public Expression postprocess(PostprocessContext ppctx) {
            FunctionEnv functionEnv = (FunctionEnv) ppctx;
            VariableDecl decl = functionEnv.getVariableDecl(varId);
            String watName = functionEnv.getVariableWAT(varId);
            assert decl != null;
            if (address)
                return new GetLocal(NumType.I32, watName);
            else if (decl.inStack && !decl.isArray && !decl.type.is_struct())
                return memory_load(decl.type, new GetLocal(decl.type.asNumType(), watName));
            else
                return new GetLocal(decl.type.is_struct()? NumType.I32 : decl.type.asNumType(), watName);
        }
    }

    static class DelayedMemoryOffset extends Expression_Delayed {
        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public String toString() {
            return "DelayedMemoryOffset";
        }

        @Override
        public Expression postprocess(PostprocessContext ppctx) {
            if (ppctx instanceof ModuleEnv)
                return new Const(((ModuleEnv) ppctx).STACK_SIZE + ((ModuleEnv) ppctx).data.size());
            else
                return this;
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
        final String block_id;
        final Map<String,String> varNameToId;
        BlockEnv (String block_id) {
            this.block_id = block_id;
            varNameToId = new HashMap<>();
        }
    }

    static class LoopEnv extends BlockEnv {
        Instruction block_postfix;
        boolean has_breaks;
        boolean in_initialization;
        LoopEnv(String block_id) {
            super(block_id);
            this.block_postfix = null;
            has_breaks = false;
            in_initialization = false;
        }

        void addUpdate(Instruction block_postfix) {
            this.block_postfix = block_postfix;
        }
    }

    @Override
    public ModuleEnv visitModule(c4waParser.ModuleContext ctx) {
        List<OneFunction> functions = new ArrayList<>();

        for (var g : ctx.global_decl()) {
            Partial parseGlobalDecl = visit(g);

            if (parseGlobalDecl instanceof FunctionDecl) {
                String err = moduleEnv.addDeclaration((FunctionDecl) parseGlobalDecl);
                if (err != null)
                    throw fail(g, "module", err);
            }
            else if (parseGlobalDecl instanceof OneFunction) {
                String err = moduleEnv.addDeclaration(((OneFunction) parseGlobalDecl).func.makeDeclaration());
                if (err != null)
                    throw fail(g, "module", err);
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
            if (!(rhs.expression instanceof Const || rhs.expression instanceof DelayedMemoryOffset))
                throw fail(ctx, "global_variable", "RHS '" + ctx.expression().getText() + "' hasn't evaluated to a constant");
            decl.initialValue = GenericCast.cast(rhs.type.asNumType(), decl.type.asNumType(), rhs.type.is_signed(), rhs.expression);
            if (!decl.mutable)
                decl.imported = false;
        }

        return decl;
    }

    @Override
    public FunctionDecl visitGlobal_decl_function(c4waParser.Global_decl_functionContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());
        CType[] params = ctx.variable_type().stream().map(this::visit).toArray(CType[]::new);

        FunctionDecl.SType storage = ctx.EXTERN() != null? FunctionDecl.SType.EXTERNAL :
                                     ctx.STATIC() != null? FunctionDecl.SType.STATIC :
                                        FunctionDecl.SType.IMPORTED;
        boolean anytype = false;

        if (storage == FunctionDecl.SType.IMPORTED) {
            anytype = params.length == 0;
            if (params.length == 1 && params[0] == null) // no_arg_func(void)
                params = new CType[0];
        }

        if (Arrays.stream(params).anyMatch(Objects::isNull))
            throw fail(ctx, "function_decl", "can't have void argument (unless it's the only one");


        return anytype
                ? new FunctionDecl(variableDecl.name, variableDecl.type, null, true, storage)
                : new FunctionDecl(variableDecl.name, variableDecl.type, params, false, storage)
                ;
    }

    @Override
    public OneFunction visitFunction_definition(c4waParser.Function_definitionContext ctx) {
        VariableDecl funcDecl = (VariableDecl) visit(ctx.variable_decl());
        ParamList paramList = (ctx.param_list() == null)?new ParamList(): (ParamList) visit(ctx.param_list());

        functionEnv = new FunctionEnv(funcDecl.name, funcDecl.type, moduleEnv, ctx.EXTERN() != null);

        Arrays.stream(paramList.paramList).forEach(x -> functionEnv.registerVar(x.name,
                null, x.type, true, true));

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

        if (type == null && variableWrapper.ref_level > 0)
            throw fail(ctx.primitive(), "variable_decl", "void type isn't allowed here");

        for (int i = 0; i < variableWrapper.ref_level; i ++)
            type = type.make_pointer_to();
        return new VariableDecl(type, variableWrapper.name, true);
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
            List<Instruction> res = new ArrayList<>();
            Partial parsedElem = visit(ctx.element());

            if (parsedElem instanceof OneInstruction)
                res.add(((OneInstruction) parsedElem).instruction);
            else if (!(parsedElem instanceof NoOp))
                throw new RuntimeException("Wrong type of parsedElem = " + parsedElem);
            return new InstructionList(res.toArray(Instruction[]::new));
        }
    }

    @Override
    public InstructionList visitComposite_block(c4waParser.Composite_blockContext ctx) {
        List<Instruction> res = new ArrayList<>();

        var parent = ctx.getParent();
        if (parent instanceof c4waParser.BlockContext)
            parent = parent.getParent();
        BlockEnv blockEnv =  parent instanceof c4waParser.Element_do_whileContext    ||
                             parent instanceof c4waParser.Function_definitionContext ||
                             parent instanceof c4waParser.Element_forContext
                ? null
                : new BlockEnv(functionEnv.pushBlock());
        if (blockEnv != null)
            blockStack.push(blockEnv);

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
            else if (parsedElem instanceof InstructionList)
                res.addAll(Arrays.asList(((InstructionList) parsedElem).instructions));
            else if (!(parsedElem instanceof NoOp))
                throw new RuntimeException("Wrong type of parsedElem = " + parsedElem);
        }
        if (blockEnv != null) {
            blockStack.pop();
            functionEnv.popBlock();
        }
        return new InstructionList(res.toArray(Instruction[]::new));
    }
    
    @Override
    public Partial visitStatement(c4waParser.StatementContext ctx) {
        if (ctx.getChildCount() == 1)
            return visitChildren(ctx);

        OneInstruction e1 = (OneInstruction) visit(ctx.statement(0));
        OneInstruction e2 = (OneInstruction) visit(ctx.statement(1));

        return new OneInstruction(new DelayedList(List.of(e1.instruction, e2.instruction)));
    }

    @Override
    public OneInstruction visitElement_do_while(c4waParser.Element_do_whileContext ctx) {
        OneExpression condition = (OneExpression) visit(ctx.expression());

        String block_id = functionEnv.pushBlock();
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        LoopEnv loopEnv = new LoopEnv(block_id);
        blockStack.push(loopEnv);
        InstructionList body = (InstructionList) visit(ctx.block());
        blockStack.pop();
        functionEnv.popBlock();

        List<Instruction> body_elems = new ArrayList<>(Arrays.asList(body.instructions));

        if (condition.expression instanceof Const) {
            if (((Const) condition.expression).isTrue())
                body_elems.add(new Br(block_id_cont));
        }
        else
            body_elems.add(new BrIf(block_id_cont, condition.expression));
        if (loopEnv.has_breaks)
            return new OneInstruction(
                    new Block(block_id_break, new Instruction[]{
                            new Loop(block_id_cont, body_elems.toArray(Instruction[]::new))}));
        else
            return new OneInstruction(new Loop(block_id_cont, body_elems.toArray(Instruction[]::new)));
    }

    @Override
    public OneInstruction visitElement_for(c4waParser.Element_forContext ctx) {
        String block_id = functionEnv.pushBlock();
        String block_id_cont = block_id + CONT_SUFFIX;
        String block_id_break = block_id + BREAK_SUFFIX;

        LoopEnv loopEnv = new LoopEnv(block_id);
        blockStack.push(loopEnv);
        loopEnv.in_initialization = true;
        OneInstruction initializationStatement = (OneInstruction) visit(ctx.pre);
        loopEnv.in_initialization = false;
        OneExpression testExpression = (OneExpression) visit(ctx.expression());
        OneInstruction updateStatement = (OneInstruction) visit(ctx.post);

        if (ctx.post != null)
            loopEnv.addUpdate(updateStatement.instruction);

        InstructionList body = (InstructionList) visit(ctx.block());
        blockStack.pop();
        functionEnv.popBlock();

        List<Instruction> loop_elems = new ArrayList<>();
        if (testExpression != null)
            loop_elems.add(new BrIf(block_id_break, testExpression.expression.Not(testExpression.type.asNumType())));
        loop_elems.addAll(Arrays.asList(body.instructions));
        if (updateStatement != null)
            loop_elems.add(updateStatement.instruction);
        loop_elems.add(new Br(block_id_cont));

        List<Instruction> block_elems = new ArrayList<>();
        if (initializationStatement != null)
            block_elems.add(initializationStatement.instruction);
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
        LoopEnv loopEnv = null;
        for (var b : blockStack)
            if (b instanceof LoopEnv) {
                loopEnv = (LoopEnv) b;
                break;
            }

        if (loopEnv == null)
            throw fail(ctx, is_break ? "break" : "continue", "not inside a loop");

        String ref = loopEnv.block_id + (is_break ? BREAK_SUFFIX : CONT_SUFFIX);
        if (is_break)
            loopEnv.has_breaks = true;

        if (loopEnv.block_postfix == null || is_break) {
            if (condition == null)
                return new OneInstruction(new Br(ref));
            else
                return new OneInstruction(new BrIf(ref, condition.expression));
        }
        else {
            if (condition == null)
                return new OneInstruction(new DelayedList(List.of(loopEnv.block_postfix, new Br(ref))));
            else
                return new OneInstruction(
                        new IfThenElse(condition.expression,
                        new Instruction[]{loopEnv.block_postfix, new Br(ref)}, null));
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

        Expression exp = GenericCast.cast(condition.type.asNumType(), NumType.I32, false, condition.expression);
        return new OneExpression(
                thenExp.expression.complexity() < ModuleEnv.IF_THEN_ELSE_SHORT_CIRCUIT_THRESHOLD &&
                elseExp.expression.complexity() < ModuleEnv.IF_THEN_ELSE_SHORT_CIRCUIT_THRESHOLD
                    ? new Select(exp, thenExp.expression, elseExp.expression)
                    : new IfThenElseExp(exp, thenExp.type.asNumType(), thenExp.expression,
                elseExp.expression), thenExp.type);
    }

    private OneInstruction if_then_else(ParserRuleContext ctx, OneExpression condition, InstructionList thenList,
                                        InstructionList elseList) {

        return new OneInstruction(new IfThenElse(GenericCast.cast(condition.type.asNumType(), NumType.I32, false, condition.expression),
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

        if (List.of("min", "max").contains(fname)) {
            if (args.expressions.length != 2)
                throw fail(ctx, "function_call", "function '" + fname + "' expects 2 arguments, received " +
                        args.expressions.length);
            OneExpression arg1 = args.expressions[0];
            OneExpression arg2 = args.expressions[1];

            if (!(arg1.type.is_primitive() && arg2.type.same(arg1.type)))
                throw fail (ctx, "function_call", "Arguments to '" + fname + "' must be same primitive type");

            CType type = arg1.type;
            Expression res = type.is_int()
                            ? new CallExp(moduleEnv.library("@" + fname + (type.is_32()? "_32": "_64") + (type.is_signed()? "s": "u")), type.asNumType(),
                                new Expression[]{arg1.expression, arg2.expression})
                            : new MinMax(type.asNumType(), "min".equals(fname), arg1.expression, arg2.expression);
            return new OneExpression(res, type);
        }
        else if (List.of("ceil", "floor", "fabs", "sqrt").contains(fname)) {
            if (args.expressions.length != 1)
                throw fail(ctx, "function_call", "function '" + fname + "' expects 1 argument, received " +
                        args.expressions.length);
            OneExpression arg = args.expressions[0];
            Expression res =
                    "ceil".equals(fname) ? new Ceil(arg.type.asNumType(), arg.expression) :
                    ("floor".equals(fname) ? new Floor(arg.type.asNumType(), arg.expression) :
                    ("fabs".equals(fname) ? new Abs(arg.type.asNumType(), arg.expression) :
                    ("sqrt".equals(fname) ? new Sqrt(arg.type.asNumType(), arg.expression) :
                    null)));
            assert res != null;
            return new OneExpression(res, arg.type);
        }
        else if (List.of("__builtin_clz", "__builtin_ctz", "__builtin_clzl", "__builtin_ctzl",
                "__builtin_popcount", "__builtin_popcountl").contains(fname)) {
            boolean isClz = fname.startsWith("__builtin_clz");
            boolean isCtz = fname.startsWith("__builtin_ctz");
            boolean is64 = fname.endsWith("l");

            if (args.expressions.length != 1)
                throw fail(ctx, "function_call", "function '" + fname + "' expects 1 argument, received " +
                        args.expressions.length);
            OneExpression arg = args.expressions[0];
            if (is64 && !arg.type.is_i64() || !is64 && !arg.type.is_i32())
                throw fail(ctx, "function_call", "Argument to '" + fname + "' must be '" + (is64? "long":"int") + "', received " + args.expressions[0].type);

            Expression e = isClz ? new Clz(arg.type.asNumType(), arg.expression) :
                           isCtz ? new Ctz(arg.type.asNumType(), arg.expression) :
                                   new Popcnt(arg.type.asNumType(), arg.expression);
            return new OneExpression(is64? GenericCast.cast(NumType.I64, NumType.I32, false, e) : e, CType.INT);
        }
        else if ("abort".equals(fname)) {
            if (args.expressions.length != 0)
                throw fail(ctx, "function_call", "function '" + fname + "' expects 0 argument, received " +
                        args.expressions.length);
            return new OneInstruction(new Unreachable());
        }

        FunctionDecl decl = moduleEnv.funcDecl.get(fname);
        if (decl == null)
            throw fail(ctx, "function call", "Function '" + fname + "' not defined or declared");
        decl.used = true;

        if (decl.params != null && Arrays.stream(decl.params).anyMatch(Objects::isNull))
            throw fail(ctx, "function_call", "void parameters aren't allowed (function '" + fname + "')");

        functionEnv.calls.add(fname);

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
                else if (arg.type.is_struct())
                    func_call_elms.add(new Store(NumType.I32, getStack, arg.expression));
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
            assert decl.params != null;
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
                    "' from a function which is expected to return '" + functionEnv.returnType + "'");

        return new OneInstruction(new DelayedReturn(expression.expression));
    }

    @Override
    public OneInstruction visitVariable_init(c4waParser.Variable_initContext ctx) {
        VariableDecl variableDecl = (VariableDecl) visit(ctx.variable_decl());

        BlockEnv blockEnv = blockStack.peek();

        OneExpression rhs = (OneExpression) visit(ctx.expression());
        rhs = constantAssignment(ctx, variableDecl.type, rhs);

        String varId = functionEnv.registerVar(variableDecl.name, blockEnv == null? null : blockEnv.block_id,
                variableDecl.type, false, ctx.CONST() == null);
        if (varId == null)
            throw fail(ctx, "init", "variable '" + variableDecl.name + "' already defined");

        if (blockEnv != null)
            blockEnv.varNameToId.put(variableDecl.name, varId);

        return new OneInstruction(
                new DelayedList(List.of(
                    new DelayedLocalDefinition(varId, null),
                    new DelayedAssignment(ctx, blockStack.stream().noneMatch(x -> x instanceof LoopEnv && !((LoopEnv) x).in_initialization),
                            new String[]{varId}, rhs.expression, rhs.type))));
    }

    @Override
    public OneInstruction visitSimple_increment(c4waParser.Simple_incrementContext ctx) {
        String name = ctx.ID().getText();

        VariableDecl variableDecl = variableDeclByName(name);
        String varId = variableIdByName(name);

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

        return new OneInstruction(new DelayedAssignment(ctx, false, new String[]{varId}, binaryOp.expression, binaryOp.type));
    }

    @Override
    public OneInstruction visitSimple_assignment(c4waParser.Simple_assignmentContext ctx) {
        OneExpression rhs = (OneExpression) visit(ctx.expression());

        String[] names = ctx.ID().stream().map(ParseTree::getText).toArray(String[]::new);
        String lastName = names[names.length - 1];

        VariableDecl decl = variableDeclByName(lastName);
        if (decl == null)
            decl = moduleEnv.varDecl.get(lastName);

        if (decl == null)
            throw fail(ctx, "assignment", "Variable '" + lastName + "' is not defined");

        rhs = constantAssignment(ctx, decl.type, rhs);
        return new OneInstruction(new DelayedAssignment(ctx, false, Arrays.stream(names).map(this::variableIdByName).toArray(String[]::new), rhs.expression, rhs.type));
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

        if (o_type == null)
            throw fail(ctx, "variable_decl", "void type isn't allowed here");

        BlockEnv blockEnv = blockStack.peek();

        for (var v : ctx.local_variable()) {
            LocalVariable localVar = (LocalVariable) visit(v);

            CType type = o_type;
            for (int i = 0; i < localVar.ref_level; i++)
                type = type.make_pointer_to();

            String varId = functionEnv.registerVar(localVar.name, blockEnv == null? null : blockEnv.block_id,
                    localVar.size == null? type: type.make_pointer_to(), false, true);

            if (varId == null)
                throw fail(ctx, "init", "variable '" + localVar.name + "' already defined");

            if (blockEnv != null)
                blockEnv.varNameToId.put(localVar.name, varId);

            res.add(new DelayedLocalDefinition(varId, localVar.size));

            if (type.is_struct() || localVar.size != null) {
                VariableDecl decl = functionEnv.getVariableDecl(varId);
                decl.mutable = false;
                decl.inStack = true;
                decl.isArray = localVar.size != null;
                functionEnv.markAsUsingStack();
            }

/*
            if (type.is_struct() || localVar.size != null) {
                VariableDecl decl = functionEnv.getVariableDecl(varId);
                decl.mutable = false;
                decl.inStack = true;
                decl.isArray = localVar.size != null;

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
*/
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

            if (type.is_undefined_struct())
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
        VariableDecl decl = variableDeclByName(name);
        String varId = variableIdByName(name);

        if (decl == null)
            throw fail(ctx, "address of variable", "'" + name + "' is not a local variable");

        if (decl.isArray)
            throw fail(ctx, "address of variable", "'" + name + "' is array, cannot take address of an array");

        decl.inStack = true;

//        return new OneExpression(new GetLocal(NumType.I32, name), decl.type.make_pointer_to());
        return new OneExpression(new DelayedLocalAccess(varId, true), decl.type.make_pointer_to());
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

        if (type.is_undefined_struct())
            type = resolveStruct(ctx, type);

        Expression memAddress = (type.size() == 1)
                ? new Add(NumType.I32, ptr.expression, idx.expression)
                : new Add(NumType.I32, ptr.expression, new Mul(NumType.I32, idx.expression, new Const(type.size())));

        if (type.is_struct())
            return new OneExpression(memAddress, type);
        else
            return new OneExpression(memory_load(type, memAddress), type);
    }

    @Override
    public OneExpression visitExpression_binary_cmp(c4waParser.Expression_binary_cmpContext ctx) {
        OneExpression arg1 = (OneExpression) visit(ctx.expression(0));
        OneExpression arg2 = (OneExpression) visit(ctx.expression(1));
        return binary_op(ctx, arg1, arg2, ctx.op.getText());
    }

    @Override
    public OneExpression visitExpression_binary_equal(c4waParser.Expression_binary_equalContext ctx) {
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
                                exp[i].expression.Not(exp[i].type.asNumType()) :
                            exp[i].type.is_i64() ?
                                GenericCast.cast(exp[i].type.asNumType(), NumType.I32, false, exp[i].expression)
                            :
                                exp[i].expression;
        }

        if (exp.length == 2)
            return new OneExpression(new IfThenElseExp(condition[0], NumType.I32,
                    new Const(is_and? 0 : 1),
                    new Cmp(exp[1].type.asNumType(), false, exp[1].expression,
                                    new Const(exp[1].type.asNumType(), 0))), CType.INT);

        String block_id = functionEnv.pushBlock();
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
            if (type.is_undefined_struct())
                type = resolveStruct(ctx, type);
            return new OneExpression((type.size() == 1)
                    ? new Add(NumType.I32, arg1.expression, arg2.expression)
                    : new Add(NumType.I32, arg1.expression, new Mul(NumType.I32, arg2.expression, new Const(type.size()))),
                    arg1.type);
        }
        if ("-".equals(op) && arg2.type.same(CType.INT) && arg1.type.deref() != null) {
            CType type = arg1.type.deref();
            if (type.is_undefined_struct())
                type = resolveStruct(ctx, type);
            return new OneExpression((type.size() == 1)
                    ? new Sub(NumType.I32, arg1.expression, arg2.expression)
                    : new Sub(NumType.I32, arg1.expression, new Mul(NumType.I32, arg2.expression, new Const(type.size()))),
                    arg1.type);
        }
        else if ("+".equals(op) && arg1.type.same(CType.INT) && arg2.type.deref() != null) {
            CType type = arg2.type.deref();
            if (type.is_undefined_struct())
                type = resolveStruct(ctx, type);
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

        if (List.of("<", "<=", ">", ">=", "==", "!=").contains(op))
            resType = CType.INT;
        else
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
            return new OneExpression(exp.expression.Not(exp.type.asNumType()), CType.INT);
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
    public OneExpression visitExpression_cast(c4waParser.Expression_castContext ctx) {
        OneExpression exp = (OneExpression) visit(ctx.expression());
        CType castToType = (CType) visit(ctx.variable_type());

        if (castToType == null)
            throw fail(ctx.variable_type(), "cast", "void type isn't allowed here");

        boolean signed;
        if (exp.type.is_int())
            signed = exp.type.is_signed();
        else if (castToType.is_int())
            signed = castToType.is_signed();
        else
            signed = true;

        if (exp.type.is_struct() || castToType.is_struct())
            throw fail(ctx, "cast", "cannot cast from struct or to struct");

        return new OneExpression(GenericCast.cast(exp.type.asNumType(), castToType.asNumType(), signed, exp.expression), castToType);
    }

    @Override
    public OneExpression visitExpression_variable(c4waParser.Expression_variableContext ctx) {
        return accessVariable(ctx, ctx.ID().getText());
    }

    private OneExpression accessVariable(ParserRuleContext ctx, String name) {
        if (name.equals("__builtin_offset"))
            return new OneExpression(new DelayedMemoryOffset(), CType.INT);
        else if (name.equals("__builtin_memory"))
            return new OneExpression(new Const(0), CType.CHAR.make_pointer_to());

        if (functionEnv == null)
            throw fail(ctx, "variable", "Cannot access variable '" + name + "' outside of function definition");

        VariableDecl decl = variableDeclByName(name);
        String varId = variableIdByName(name);

        if (decl != null)
            return new OneExpression(new DelayedLocalAccess(varId, false), decl.type);

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
        List<Byte> bytes = new ArrayList<>();
        for (var s : ctx.STRING())
            bytes.addAll(unescapeString(ctx, s.getText()));

        int str_id = moduleEnv.addString(bytes);
        return new OneExpression(new Const(str_id), CType.CHAR.make_pointer_to());
    }

    @Override
    public OneExpression visitExpression_character(c4waParser.Expression_characterContext ctx) {
        String s = ctx.CHARACTER().getText();
        byte val = unescapeChar(ctx, s);
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

    private VariableDecl variableDeclByName(String name) {
        for(BlockEnv b : blockStack) {
            String varId = b.varNameToId.get(name);
            if (varId != null)
                return functionEnv.getVariableDecl(varId);
        }
        return functionEnv.getVariableDecl(name);
    }

    private String variableIdByName(String name) {
        for(BlockEnv b : blockStack) {
            String varId = b.varNameToId.get(name);
            if (varId != null)
                return varId;
        }
        return name;
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

    static private OneExpression constantAssignment(ParserRuleContext ctx, CType lhsType, OneExpression rhs) {
        if (!lhsType.isValidRHS(rhs.type)) {
            if (rhs.expression instanceof Const && (lhsType.is_primitive() || (((Const) rhs.expression).isZero() && lhsType.is_ptr())))
                return new OneExpression(new Const(lhsType.asNumType(), (Const) rhs.expression), lhsType);
            else
                throw fail(ctx, "init", "Expression of type " + rhs.type + " cannot be assigned to variable of type " + lhsType);
        }

        return rhs;
    }

    private Struct resolveStruct(ParserRuleContext ctx, CType type) {
        String name = ((StructDecl) type).name;
        Struct realStruct = moduleEnv.structs.get(name);
        if (realStruct == null)
            throw fail(ctx, "type", type + " is not defined");
        return realStruct;
    }

    private static byte unescapeChar(ParserRuleContext ctx, String str) {
        Byte res = unescapeBytes(str.substring(1, str.length() - 1).getBytes(StandardCharsets.UTF_8));
        if (res == null)
            throw fail(ctx, "character", "Invalid character '" + str + "'");
        return res;
    }

    private static Byte unescapeBytes(byte[] bytes) {
        if (bytes.length == 1)
            return bytes[0];
        else if (bytes[0] != '\\')
            return null;
        else if (bytes[1] == 'a' && bytes.length == 2)
            return 0x07;
        else if (bytes[1] == 'b' && bytes.length == 2)
            return 0x08;
        else if (bytes[1] == 'e' && bytes.length == 2)
            return 0x1B;
        else if (bytes[1] == 'f' && bytes.length == 2)
            return 0x0C;
        else if (bytes[1] == 'n' && bytes.length == 2)
            return 0x0A;
        else if (bytes[1] == 'r' && bytes.length == 2)
            return 0x0D;
        else if (bytes[1] == 't' && bytes.length == 2)
            return 0x09;
        else if (bytes[1] == 'v' && bytes.length == 2)
            return 0x0B;
        else if (bytes[1] == '\\' && bytes.length == 2)
            return 0x5C;
        else if (bytes[1] == '\'' && bytes.length == 2)
            return 0x27;
        else if (bytes[1] == '"' && bytes.length == 2)
            return 0x22;
        else if (bytes[1] == '?' && bytes.length == 2)
            return 0x3F;
        else if ('0' <= bytes[1] && bytes[1] <= '7' && bytes.length <= 4) {
            int res = bytes.length == 4 ? (bytes[1] - '0')*64 + (bytes[2] - '0') * 8 + (bytes[3] - '0')
                    : (bytes.length == 3 ? (bytes[1] - '0')*8 + (bytes[2] - '0') : bytes[1] - '0');
            if (res >= 256)
                return null;
            return (byte)res;
        }
        else if (bytes[1] == 'x' && bytes.length == 4) {
            try {
                return (byte)Integer.parseInt(Character.toString(bytes[2]) + Character.toString(bytes[3]), 16);
            }
            catch (NumberFormatException _ignore) {
                return null;
            }
        }
        else
            return null;
    }

    private static List<Byte> unescapeString(ParserRuleContext ctx, String str) {
        int N = str.length() - 1;
        List<Byte> res = new ArrayList<>();
        int i = 1;
        while(i < N) {
            if (str.charAt(i) == '\\') {
                byte escSymbol = -1;
                int i1 = -1;
                for (int j = i + 1; j < N && j < i + 5; j ++) {
                    Byte t = unescapeBytes(str.substring(i, j + 1).getBytes(StandardCharsets.UTF_8));
                    if (t != null) {
                        escSymbol = t;
                        i1 = j + 1;
                    }
                }
                if (i1 < 0)
                    throw fail(ctx, "string", "Invalid escape at position " + (i + 1));

                res.add(escSymbol);
                i = i1;
            }
            else {
                int j;
                for (j = i + 1; j < N && str.charAt(j) != '\\'; j++);
                byte[] bytes = str.substring(i, j).getBytes(StandardCharsets.UTF_8);
                for (byte b: bytes)
                    res.add(b);
                i = j;
            }
        }
        return res;
    }

    static String unescape_TBR(String str) {
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
