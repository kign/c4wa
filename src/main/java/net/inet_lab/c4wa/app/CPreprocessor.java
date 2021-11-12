package net.inet_lab.c4wa.app;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CPreprocessor {
    public static boolean hasDirectives(String src) {
        Pattern ppp = Pattern.compile("^#define", Pattern.MULTILINE);
        Matcher m = ppp.matcher(src);
        return m.find();
    }

    public static String run(String src) throws IOException {
        File tempFile = File.createTempFile("c4wa-", "-temp.c");
        tempFile.deleteOnExit();
        FileWriter w = new FileWriter(tempFile);
        w.write(src);
        w.close();
        // https://gcc.gnu.org/onlinedocs/gcc/Preprocessor-Options.html
        // -E invokes preprocessor
        // -P inhibits line markers
        // -C preserves comments
        Process process = Runtime.getRuntime().exec("gcc -E -P -C " + tempFile.getAbsolutePath());
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
    }

    public static String readAndProcess(String fileName) throws IOException {
        String programText = Files.readString(Path.of(fileName));
        if (hasDirectives(programText))
            return run(programText);
        else
            return programText;
    }
}
