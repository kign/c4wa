package net.inet_lab.c4wa.app;

import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import net.inet_lab.c4wa.autogen.parser.c4waLexer;
import net.inet_lab.c4wa.autogen.parser.c4waParser;

import net.inet_lab.c4wa.transpile.ParserTreeVisitor;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Hey!");

        // c4waLexer lexer = new c4waLexer(CharStreams.fromString("1+2-5"));
        c4waLexer lexer = new c4waLexer(CharStreams.fromFileName("tests/test1.c"));

        c4waParser parser = new c4waParser(new CommonTokenStream(lexer));

        ParseTree tree = parser.module();
        System.out.println("Parser returned " + tree.toStringTree(parser));

        ParserTreeVisitor v = new ParserTreeVisitor();

        String result = v.visit(tree);

        System.out.println("Visitor returned " + result);
    }
}
