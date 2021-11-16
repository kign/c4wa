package net.inet_lab.c4wa.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.inet_lab.c4wa.transpile.SyntaxError;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import net.inet_lab.c4wa.autogen.parser.c4waLexer;
import net.inet_lab.c4wa.autogen.parser.c4waParser;
import net.inet_lab.c4wa.transpile.ParseTreeVisitor;
import net.inet_lab.c4wa.transpile.ModuleEnv;

public class Main {
    private static class Option {
        final Character shortName;
        final String longName;
        final boolean needsArg;
        Option(Character shortName, String longName, boolean needsArg) {
            this.shortName = shortName;
            this.longName = longName;
            this.needsArg = needsArg;
        }
        Option(Character shortName, String longName) {
            this(shortName, longName, false);
        }
        String name () {
            return longName == null? shortName.toString(): longName;
        }
    }

    public static void main(String[] args) throws IOException {
        Properties prop = defaultProperties();
        String appName = prop.getProperty("appName");

        List<String> ppOptions = new ArrayList<>();
        Map<String, String> parsedArgs = new HashMap<>();
        List<String> fileArgs = new ArrayList<>();

        String error = parseCommandLineArgs(args,
                List.of(new Option('o', "output", true),
                        new Option('h', "help"),
                        new Option('P', null)),
                parsedArgs, fileArgs, prop, ppOptions);

        if (error != null) {
            System.err.println("Error parsing command line: " + error);
            System.exit(1);
        }

        String usage = "Usage: " + appName + " [OPTIONS] <FILE.c>";
        if (parsedArgs.containsKey("help")) {
            printUsage(prop, usage);
            System.exit(0);
        }

        if (fileArgs.size() != 1) {
            if (fileArgs.isEmpty())
                System.err.println(usage);
            else
                System.err.println("There must be exactly one argument, " + fileArgs.size() + " found\nUse "
                        + appName + " -help for help");
            System.exit(1);
        }

        String fileName = fileArgs.get(0);

        if (!((new File(fileName)).exists())) {
            System.err.println("File " + fileName + " doesn't exist");
            System.exit(1);
        }

        String output = parsedArgs.containsKey("output")
            ? parsedArgs.get("output")
            : Paths.get(fileName).getFileName().toString().replaceFirst("[.][^.]+$", "") + ".wat";

        boolean makeWasm = output.endsWith(".wasm");

        List<String> programLines = parsedArgs.containsKey("P")
            ? runFileThroughCPreprocessor(fileName, ppOptions)
            : Files.lines(Paths.get(fileName), StandardCharsets.UTF_8).collect(Collectors.toUnmodifiableList());

        String programText = String.join("\n", programLines);

        if (!parsedArgs.containsKey("P") && hasPreprocessorDirectives(programText))
            System.err.println("WARNING: your source file contains preprocessor directives, use -P option");

        try {
            ParseTree tree = buildParseTree(programText);
            String wat = generateWAT(tree, prop);

            if (makeWasm) {
                System.err.println("Compiling WAT to WASM is not yet implemented");
                System.exit(1);
            }

            if ("-".equals(output))
                System.out.println(wat);
            else {
                (new PrintStream(output)).println(wat);
            }
        }
        catch(SyntaxError err) {
            if (err.line_st != err.line_en) {
                System.err.println("Error: " + err.msg);
                System.out.println("Error begins at line " + err.line_st + "(position " + err.pos_st + ") and ends at line " +
                        err.line_en + "(position " + err.pos_en + "); we can't handle it yet");
            }
            else {
                System.out.println(programLines.get(err.line_st - 1));
                System.out.println(" ".repeat(err.pos_st) + "^".repeat(1 + err.pos_en - err.pos_st));
                System.err.println("Syntax error[" + err.line_st + ":" + err.pos_st + "]: " + err.msg);
            }

            System.exit(1);
        }
    }

    // to be used from tests
    public static void runAndSave(String programText, boolean usePreprocessor, Path watFileName) throws IOException {
        Properties prop = defaultProperties();

        if (usePreprocessor)
            programText = String.join("\n", runTextThroughCPreprocessor(programText, List.of()));

        ParseTree tree = buildParseTree(programText);
        String wat = generateWAT(tree, prop);

        PrintStream out = new PrintStream(watFileName.toFile());
        out.println(wat);
    }

    private static Properties defaultProperties () throws IOException {
        Properties prop = new Properties();
        prop.load(Main.class.getClassLoader().getResourceAsStream("gradle.properties"));

        return prop;
    }

    private static void printUsage(Properties prop, String usage) {
        System.out.print(usage + "\n" +
                "\n" +
                "Options are: \n" +
                "\n" +
                " -P            invoke C preprocessor via GCC\n" +
                " -Dname=value  when option -P is used, pass definition to C preprocessor\n" +
                " -Xname=value  define compiler property (see below)\n" +
                " -o, --output <FILE>  specify output files, either .wat or .wasm (or - for stdout)\n" +
                " -k, --keep    when compiling to WASM, keep intermediate WAT file\n" +
                " -h, --help    this help screen\n" +
                "\n" +
                "Compiler properties:\n" +
                "\n" +
                "Name                             Default value\n" +
                "----------------------------------------------\n"
        );
        @SuppressWarnings("unchecked")
        Enumeration<String> enums = (Enumeration<String>) prop.propertyNames();
        while (enums.hasMoreElements()) {
            String key = enums.nextElement();
            if (key.indexOf('.') > 0) {
                String value = prop.getProperty(key);
                System.out.printf("%-30s : %s\n", key, value);
            }
        }
    }

    private static String parseCommandLineArgs(String[] args, List<Option> options, Map<String, String> parsedArgs, List<String> fileArgs, Properties prop, List<String> ppOptions) {
        for (int i = 0; i < args.length; i++) {
            String o = args[i];
            if (o.startsWith("-D"))
                ppOptions.add(o);
            else if (o.startsWith("-X")) {
                int j = o.indexOf('=');
                if (j < 0)
                    return "Invalid option " + o;
                prop.setProperty(o.substring(2, j), o.substring(j + 1));
            } else if (o.charAt(0) == '-' && o.length() >= 2 && o.charAt(1) != '-') {
                for (int j = 1; j < o.length(); j++) {
                    char so = o.charAt(j);
                    var g = options.stream().filter(x -> x.shortName == so).findFirst();
                    if (g.isEmpty())
                        return "Invalid option '" + so + "' in " + o;
                    String value = "yes";
                    if (g.get().needsArg) {
                        if (o.length() > 2 || i == args.length - 1)
                            return "Option '" + so + "' in " + o + " must have argument";
                        value = args[i + 1];
                        i++;
                    }
                    String name = g.get().name();
                    if (parsedArgs.containsKey(name))
                        return "Option --" + name + " already defined";
                    parsedArgs.put(name, value);
                }
            } else if (o.startsWith("--")) {
                var g = options.stream().filter(x -> x.longName != null && (o.equals("--" + x.longName) || o.startsWith("--" + x.longName + "="))).findFirst();
                if (g.isEmpty())
                    return "Invalid option " + o;
                String value = "yes";
                if (o.startsWith("--" + g.get().longName + "=")) {
                    if (!g.get().needsArg)
                        return "Option " + o + " doesn't expect an argument";
                    value = o.substring(g.get().longName.length() + 3);
                } else if (g.get().needsArg) {
                    if (i == args.length - 1)
                        return "Option " + o + " must have argument";
                    value = args[i + 1];
                    i++;
                }
                String name = g.get().name();
                if (parsedArgs.containsKey(name))
                    return "Option --" + name + " already defined";
                parsedArgs.put(name, value);
            } else
                fileArgs.add(o);
        }

        return null;
    }

    private static List<String> runFileThroughCPreprocessor(String fileName, List<String> ppOptions) throws IOException {
        // https://gcc.gnu.org/onlinedocs/gcc/Preprocessor-Options.html
        // -E invokes preprocessor
        // -P inhibits line markers
        // -C preserves comments
        String command = "gcc -E -P -C " + String.join(" ", ppOptions) + fileName;
        Process process = Runtime.getRuntime().exec(command);
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.toUnmodifiableList());
    }

    private static List<String> runTextThroughCPreprocessor(String src, List<String> ppOptions) throws IOException {
        File tempFile = File.createTempFile("c4wa-", "-temp.c");
        tempFile.deleteOnExit();
        FileWriter w = new FileWriter(tempFile);
        w.write(src);
        w.close();
        return runFileThroughCPreprocessor(tempFile.getAbsolutePath(), ppOptions);
    }

    private static boolean hasPreprocessorDirectives(String src) {
        Pattern ppp = Pattern.compile("^#define", Pattern.MULTILINE);
        Matcher m = ppp.matcher(src);
        return m.find();
    }

    static private class ThrowingErrorListener extends BaseErrorListener {
        // https://newbedev.com/handling-errors-in-antlr4
        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            //throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
            throw new SyntaxError(line, line, charPositionInLine, charPositionInLine, msg);
        }
    }

    private static ParseTree buildParseTree(String programText) {
        c4waLexer lexer = new c4waLexer(CharStreams.fromString(programText));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        c4waParser parser = new c4waParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        ParseTree tree = parser.module();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Syntax errors were detected");
            System.exit(1);
        }

        return tree;
    }

    private static String generateWAT(ParseTree tree, Properties prop) {
            //System.out.println("Parser returned \n" + tree.toStringTree(parser));
            ParseTreeVisitor v = new ParseTreeVisitor(prop);
            ModuleEnv result = (ModuleEnv) v.visit(tree);
            return result.wat().toStringPretty(2);
    }
}
