package net.inet_lab.c4wa.transpile;

import java.io.IOException;
import java.io.PrintStream;

public class ModuleEnv implements Partial {
    FunctionEnv[] functions;

    public ModuleEnv () {}

    public void addFunctions(FunctionEnv[] functions) {
        this.functions = functions;
    }

    public void generateWat(final PrintStream out) throws IOException {
        out.println("(module ");

        for (FunctionEnv f: functions)
            f.generateWat(out);

        out.println(")");
    }
}
