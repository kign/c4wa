package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waLexer;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RunAllTests {

    @Test
    void demoTestMethod() throws IOException {
        final String ctests = "ctests";
        final var loader = Thread.currentThread().getContextClassLoader();
        assertNotNull(loader);

        BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(loader.getResourceAsStream(ctests))));
        String fileName;
        while ((fileName = br.readLine()) != null) {
            System.out.println(fileName);
            String programText = Files.readString(Path.of(Objects.requireNonNull(loader.getResource(ctests + "/" + fileName)).getPath()));
            c4waLexer lexer = new c4waLexer(CharStreams.fromString(programText));
            c4waParser parser = new c4waParser(new CommonTokenStream(lexer));
            ParseTree tree = parser.module();
            ParserTreeVisitor v = new ParserTreeVisitor();
            ModuleEnv result = (ModuleEnv) v.visit(tree);
            result.generateWat(System.out);
        }
    }
}
