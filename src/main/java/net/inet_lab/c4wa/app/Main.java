package net.inet_lab.c4wa.app;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.inet_lab.c4wa.transpile.SyntaxError;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import net.inet_lab.c4wa.autogen.cparser.c4waLexer;
import net.inet_lab.c4wa.autogen.cparser.c4waParser;
import net.inet_lab.c4wa.transpile.ParseTreeVisitor;
import net.inet_lab.c4wa.transpile.ModuleEnv;

public class Main {
    static final ClassLoader loader = Main.class.getClassLoader();

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
        List<String> builtin_libs = new ArrayList<>();

        addPreprocessorSymbolsFromProperties(ppOptions, prop);

        String error = parseCommandLineArgs(args,
                List.of(new Option('o', "output", true),
                        new Option('h', "help"),
                        new Option('P', null)),
                parsedArgs, fileArgs, prop, ppOptions, builtin_libs);

        if (error != null) {
            System.err.println("Error parsing command line: " + error);
            System.exit(1);
        }

        String usage = "Usage: " + appName + " [OPTIONS] <FILE.c> [<FILE_2.c> .... <FILE_N.c>]";
        if (parsedArgs.containsKey("help")) {
            if (parsedArgs.containsKey("P")) {
                for (String x: ppOptions)
                    System.out.println(x);
            }
            else
                printUsage(prop, usage);
            System.exit(0);
        }

        if (fileArgs.isEmpty()) {
            System.err.println(usage);
            System.exit(1);
        }

        final boolean usePP = parsedArgs.containsKey("output");

        String output =
                parsedArgs.containsKey("output") ? parsedArgs.get("output") :
                fileArgs.size() == 1 ? Paths.get(fileArgs.get(0)).getFileName().toString().replaceFirst("[.][^.]+$", "") + ".wat" :
                "bundle.wat";

        boolean makeWasm = output.endsWith(".wasm");

        if (makeWasm) {
            System.err.println("Compiling WAT to WASM is not yet implemented");
            System.exit(1);
        }

        ModuleEnv moduleEnv = new ModuleEnv(prop);
        String wat = null;
        final int n_units = fileArgs.size() + builtin_libs.size();
        for (int iarg = 0; iarg < n_units; iarg ++) {
            List<String> programLines;
            String fileName;
            if (iarg < fileArgs.size()) {
                fileName = fileArgs.get(iarg);

                if (!((new File(fileName)).exists())) {
                    System.err.println("File " + fileName + " doesn't exist");
                    System.exit(1);
                }

                programLines = usePP
                        ? runFileThroughCPreprocessor(fileName, ppOptions)
                        : Files.lines(Paths.get(fileName), StandardCharsets.UTF_8).collect(Collectors.toUnmodifiableList());

                if (programLines.size() == 0) {
                    System.err.println("file '" + fileName + "': no text, could be preprocessor error");
                    System.exit(1);
                }
            }
            else {
                String libName = builtin_libs.get(iarg - fileArgs.size());
                fileName = "<builtin>:" + libName + ".c";

                programLines = new BufferedReader(
                        new InputStreamReader(
                                Objects.requireNonNull(
                                        loader.getResourceAsStream("lib/" + libName + ".c"))))
                        .lines().collect(Collectors.toUnmodifiableList());
            }

            String programText = String.join("\n", programLines);

            if (!usePP && hasPreprocessorDirectives(programText))
                System.err.println("WARNING: file '" + fileName + "' contains preprocessor directives, use -P option");

            try {
                ParseTree tree = buildParseTree(programText);
                ParseTreeVisitor v = new ParseTreeVisitor(moduleEnv);
                v.visit(tree);
                if (iarg == n_units - 1)
                    wat = moduleEnv.wat().toStringPretty(2);
            } catch (SyntaxError err) {
                int lineno = err.line_st;
                if (lineno < 0)
                    System.err.println(err.msg);
                else {
                    String realName = fileName;
                    if (usePP && iarg < fileArgs.size() || programLines.get(0).charAt(0) == '#') {
                        err.locate(programLines);
                        realName = err.fileName;
                        lineno = err.lineno;
                    }

                    String errLine = programLines.get(err.line_st - 1);
                    System.out.println(errLine);
                    int pos_en = err.line_st == err.line_en? err.pos_en : (errLine.length() - 1);
                    System.out.println(" ".repeat(err.pos_st) + "^".repeat(1 + pos_en - err.pos_st));
                    System.err.println("[" + realName + ":" + lineno + "] " + err.msg);

                    if (err.line_st != err.line_en)
                        System.out.println("(Actual reported error location is " + (err.line_en - err.line_st + 1) + " lines long)");
                }
                System.exit(1);
            }
        }

        if ("-".equals(output))
            System.out.println(wat);
        else
            (new PrintStream(output)).println(wat);
    }

    // to be used from tests
    public static void runAndSave(String programText, boolean usePreprocessor, List<String> libs, Path watFileName) throws IOException {
        Properties prop = defaultProperties();

        List<String> ppOptions = new ArrayList<>();

        addPreprocessorSymbolsFromProperties(ppOptions, prop);

        if (usePreprocessor)
            programText = String.join("\n", runTextThroughCPreprocessor(programText, ppOptions));

        ModuleEnv moduleEnv = new ModuleEnv(prop);

        ParseTree tree = buildParseTree(programText);
        ParseTreeVisitor v = new ParseTreeVisitor(moduleEnv);
        v.visit(tree);

        for (String libName: libs) {
            var programLines = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    loader.getResourceAsStream("lib/" + libName + ".c"))))
                    .lines().collect(Collectors.toUnmodifiableList());
            programText = String.join("\n", programLines);

            tree = buildParseTree(programText);
            v = new ParseTreeVisitor(moduleEnv);
            v.visit(tree);
        }

        PrintStream out = new PrintStream(watFileName.toFile());
        out.println(moduleEnv.wat().toStringPretty(2));
    }

    private static Properties defaultProperties () throws IOException {
        Properties prop = new Properties();
//        prop.load(Main.class.getClassLoader().getResourceAsStream("gradle.properties"));
        prop.load(loader.getResourceAsStream("gradle.properties"));

        return prop;
    }

    private static void addPreprocessorSymbolsFromProperties(List<String> ppOptions, Properties prop) {
        ppOptions.add("-DC4WA");
        ppOptions.add("-DC4WA_VERSION=" + prop.getProperty("appVersion"));
        ppOptions.add("-DC4WA_STACK_SIZE=" + prop.getProperty("module.stackSize"));
    }

    private static void printUsage(Properties prop, String usage) {
        String appVersion = prop.getProperty("appVersion");
        String appDate = prop.getProperty("appDate");
        System.out.print(
                "Subset of C to WAT/WASM compiler, version " + appVersion + ", " + appDate + "\n" +
                "\n" +
                usage + "\n" +
                "\n" +
                "Options are: \n" +
                "\n" +
                " -P              invoke C preprocessor via GCC (use -Ph to print predefined symbols)\n" +
                " -D<name>=value  when option -P is used, pass definition to C preprocessor\n" +
                " -X<name>=value  define compiler property (see below)\n" +
                " -l<name>        include built-in library <name> (use -lh to list available libraries)\n" +
                " -o, --output <FILE>  specify output files, either .wat or .wasm (or - for stdout)\n" +
                " -k, --keep      when compiling to WASM, keep intermediate WAT file\n" +
                " -h, --help      this help screen\n" +
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

    private static String parseCommandLineArgs(String[] args, List<Option> options, Map<String, String> parsedArgs,
                                               List<String> fileArgs, Properties prop,
                                               List<String> ppOptions, List<String> builtin_libs) throws IOException {
        for (int i = 0; i < args.length; i++) {
            String o = args[i];
            if (o.startsWith("-D"))
                ppOptions.add(o);
            else if (o.startsWith("-X")) {
                int j = o.indexOf('=');
                if (j < 0)
                    return "Invalid option " + o;
                String p = o.substring(2, j);
                if (prop.getProperty(p) == null)
                    return "No such property '" + p + "'";
                prop.setProperty(o.substring(2, j), o.substring(j + 1));
            }
            else if (o.equals("-lh")) {
                System.out.println("Library name   Description\n----------------------------------------------------------");
                try {
                    for (String libName: getResourceListing(Main.class, "lib"))
                        if (libName.endsWith(".c")) {
                            String line = new BufferedReader(
                                    new InputStreamReader(
                                            Objects.requireNonNull(
                                                    loader.getResourceAsStream("lib/" + libName))))
                                    .lines().filter(x -> x.startsWith("//")).findFirst().orElse("// <Empty file>").substring(3);
                            System.out.printf("%-15s%s\n", libName.substring(0,libName.length() - 2), line);
                        }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                System.exit(0);
            }
            else if (o.startsWith("-l") && o.length() > 2)
                builtin_libs.add(o.substring(2));
            else if (o.charAt(0) == '-' && o.length() >= 2 && o.charAt(1) != '-') {
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
        // -P inhibits line markers [no longer needed]
        // -C preserves comments
        String command = "gcc -E -C " + String.join(" ", ppOptions) + " " + fileName;
        Process process = Runtime.getRuntime().exec(command);
        List<String> output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.toUnmodifiableList());

        String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (error.length() > 0) {
            System.err.println("Preprocessor encountered errors");
            System.out.println(error);
            System.exit(1);
        }

        return output;
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

        if (parser.getNumberOfSyntaxErrors() > 0)
            throw new RuntimeException("Syntax errors were detected; this should never happen, as we throw exception of 1-st error");

        return tree;
    }

    private static String[] getResourceListing(Class<?> clazz, String path) throws IOException, URISyntaxException {
        // Adopted & modernized from: http://www.uofr.net/~greg/java/get-resource-listing.html
        // Basically, Java has no built-in capability to list resource directory from JAR file;
        // So there are two choices, either save content in special resource file(s) or simply open JAR file and read
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        assert dirURL != null;
        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path + "/")) { //filter according to the path
                    String entry = name.substring(1 + path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir > 0)
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);

                    if (entry.length() > 0)
                        result.add(entry);
                }
            }
            return result.toArray(String[]::new);
        }

        throw new RuntimeException("Cannot list files in " + path + " inside  " + dirURL);
    }
}
