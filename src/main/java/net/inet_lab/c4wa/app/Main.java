package net.inet_lab.c4wa.app;

import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import net.inet_lab.c4wa.autogen.parser.c4waLexer;
import net.inet_lab.c4wa.autogen.parser.c4waParser;

import net.inet_lab.c4wa.transpile.ParserTreeVisitor;
import net.inet_lab.c4wa.transpile.ModuleEnv;

public class Main {
    public static void main(String[] args) throws IOException {
        // String programText = Files.readString(Path.of("tests/test1.c"));
        // c4waLexer lexer = new c4waLexer(CharStreams.fromString(programText));

        // System.out.println("Reading " + args[0]);
        c4waLexer lexer = new c4waLexer(CharStreams.fromFileName(args[0]));

        c4waParser parser = new c4waParser(new CommonTokenStream(lexer));

        ParseTree tree = parser.module();
        // System.out.println("Parser returned " + tree.toStringTree(parser));

        ParserTreeVisitor v = new ParserTreeVisitor();

        ModuleEnv result = (ModuleEnv)v.visit(tree);

        result.generateWat(System.out);
    }
}
