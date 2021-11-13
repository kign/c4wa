package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.app.CPreprocessor;
import net.inet_lab.c4wa.app.Main;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        final var needs_pp = List.of("170-life.c");

        while ((fileName = br.readLine()) != null) {
            final String fname = fileName;
            if (!fname.endsWith(".c"))
                continue;
            tests.add(DynamicTest.dynamicTest(fileName, () -> {
                String programText = Files.readString(Path.of(Objects.requireNonNull(loader.getResource(ctests + "/" + fname)).getPath()));
                Main.runAndSave(programText, needs_pp.contains(fname), Paths.get("tests", "wat", fname.replace(".c", ".wat")));
            }));
        }

        return tests;
    }
}
