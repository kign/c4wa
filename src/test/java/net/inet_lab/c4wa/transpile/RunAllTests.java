package net.inet_lab.c4wa.transpile;

import net.inet_lab.c4wa.app.Main;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

        final var needs_pp = List.of("121-smoke.c", "170-life.c", "171-life.c");
        final var libraries = List.of("110-smoke.c", "115-smoke.c", "118-smoke.c", "119-smoke.c",
                "150-pointers.c",
                "161-struct.c", "163-struct.c", "164-struct.c",
                "mm_incr",
                "160-struct.c",
                "mm_fixed"
                );
        while ((fileName = br.readLine()) != null) {
            final String fname = fileName;
            if (!fname.endsWith(".c"))
                continue;
            List<String> libs = new ArrayList<>();
            boolean f = false;
            for (String x: libraries) {
                if (x.equals(fname))
                    f = true;
                else if (x.endsWith(".c")) {
                    if (!libs.isEmpty())
                        break;
                }
                else if (f)
                    libs.add(x);
            }
            tests.add(DynamicTest.dynamicTest(fileName, () -> {
                String programText = Files.readString(Path.of(Objects.requireNonNull(loader.getResource(ctests + "/" + fname)).getPath()));
                Main.runAndSave(programText, needs_pp.contains(fname), libs, Paths.get("tests", "wat", fname.replace(".c", ".wat")));
            }));
        }

        return tests;
    }
}
