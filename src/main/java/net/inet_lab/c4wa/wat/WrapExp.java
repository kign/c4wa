package net.inet_lab.c4wa.wat;

import java.io.IOException;

public class WrapExp extends Instruction_1 {
    public WrapExp(Expression arg) {
        super(InstructionName.SPECIAL, arg);
    }

    @Override
    public Instruction[] postprocess(PostprocessContext ppctx) {
        Expression a = arg.postprocess(ppctx);
        if (a == arg)
            return new Instruction[]{this};
        else
            return new Instruction[]{new WrapExp(a)};
    }

    @Override
    public String toString() {
        return arg.toString();
    }

    @Override
    void wasm(Module.WasmContext mCtx, Func.WasmContext fCtx, WasmOutputStream out) throws IOException {
        arg.wasm(mCtx, fCtx, out);
    }

    @Override
    public void execute(ExecutionCtx ectx) {
        throw new ExecutionFunc.ExeReturn(arg.eval(ectx));
    }
}
