package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.autogen.parser.c4waLexer;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RunAllTests {

    @TestFactory
    List<DynamicTest> generateWatFiles() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        final String ctests = "c";
        final var loader = Thread.currentThread().getContextClassLoader();
        assertNotNull(loader);

        BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(loader.getResourceAsStream(ctests))));
        String fileName;
        Files.createDirectories(Paths.get("tests", "wat"));

        while ((fileName = br.readLine()) != null) {
            final String fname = fileName;
            tests.add(DynamicTest.dynamicTest(fileName, () -> {
                String programText = Files.readString(Path.of(Objects.requireNonNull(loader.getResource(ctests + "/" + fname)).getPath()));
                c4waLexer lexer = new c4waLexer(CharStreams.fromString(programText));
                c4waParser parser = new c4waParser(new CommonTokenStream(lexer));
                ParseTree tree = parser.module();
                ParserTreeVisitor v = new ParserTreeVisitor();
                ModuleEnv result = (ModuleEnv) v.visit(tree);

                PrintStream out = new PrintStream(Paths.get("tests", "wat", fname.replace(".c", ".wat")).toFile());
                result.generateWat(out);
            }));
        }

        return tests;
    }
}
