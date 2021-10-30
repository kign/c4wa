package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waBaseVisitor;
import net.inet_lab.c4wa.autogen.parser.c4waParser;

public class ParserTreeVisitor extends c4waBaseVisitor<String> {
    @Override
    public String visitModule(c4waParser.ModuleContext ctx) {
        System.out.println("Visiting module; ");

        return "(module\n" + visit(ctx.func(0)) + ")\n";
    }

    @Override
    public String visitFunc(c4waParser.FuncContext ctx) {
        return "<Function>";
    }
}
