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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.inet_lab.c4wa.transpile.SyntaxError;
import net.inet_lab.c4wa.wat.Module;
import net.inet_lab.c4wa.wat.WasmOutputStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import net.inet_lab.c4wa.autogen.cparser.c4waLexer;
import net.inet_lab.c4wa.autogen.cparser.c4waParser;
import net.inet_lab.c4wa.transpile.ParseTreeVisitor;
import net.inet_lab.c4wa.transpile.ModuleEnv;
import org.jetbrains.annotations.Nullable;

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

    private enum WarningTreatment {
        IGNORE,
        TREAS_AS_ERRORS
    }

    public static void main(String[] args) throws IOException {
        Properties prop = defaultProperties();
        String appName = prop.getProperty("appName");
        String ppCmd = prop.getProperty("preprocessor.command");

        List<String> ppOptions = new ArrayList<>();
        Map<String, String> parsedArgs = new HashMap<>();
        List<String> fileArgs = new ArrayList<>();
        List<String> builtin_libs = new ArrayList<>();
        WarningTreatment[] warningTreatment = {null};

        String error = parseCommandLineArgs(args,
                List.of(new Option('o', "output", true),
                        new Option('h', "help"),
                        new Option('k', "keep"),
                        new Option('v', null)),
                parsedArgs, fileArgs, prop, ppOptions, builtin_libs, warningTreatment);

        final boolean forcePP = !ppOptions.isEmpty();

        addPreprocessorSymbolsFromProperties(ppOptions, prop);

        if (error != null) {
            System.err.println("Error parsing command line: " + error);
            System.exit(1);
        }

        String usage = "Usage: " + appName + " [OPTIONS] <FILE.c> [<FILE_2.c> .... <FILE_N.c>]";
        if (parsedArgs.containsKey("help")) {
            printUsage(prop, usage);
            System.exit(0);
        }

        if (fileArgs.isEmpty()) {
            System.err.println(usage);
            System.exit(1);
        }

        String _output =
                parsedArgs.containsKey("output") ? parsedArgs.get("output") :
                fileArgs.size() == 1 ? Paths.get(fileArgs.get(0)).getFileName().toString().replaceFirst("[.][^.]+$", "") + ".wasm" :
                "bundle.wasm";

        boolean is_wat_output = _output.equals("-") || _output.endsWith(".wat");
        String wasm_output = is_wat_output? null: _output;
        String wat_output = is_wat_output ? _output :
                parsedArgs.containsKey("keep")? _output.substring(0, _output.length() - 5) + ".wat" : null;

        ModuleEnv moduleEnv = new ModuleEnv(prop);
        Module wat = null;
        Pattern ppp = Pattern.compile("^#\\s*(define|if|undef|include)");
        int n_units = fileArgs.size() + builtin_libs.size();
        final int[] errors = {0};
        final int[] warnings = {0};
        final ArrayList<List<String>> programLinesCache = new ArrayList<>();

        for (int iarg = 0; iarg < n_units; iarg ++) {
            List<String> _programLines;
            String fileName;
            if (iarg < fileArgs.size()) {
                fileName = fileArgs.get(iarg);

                if (!((new File(fileName)).exists())) {
                    System.err.println("File " + fileName + " doesn't exist");
                    System.exit(1);
                }

                if (forcePP)
                    _programLines = runFileThroughCPreprocessor(ppCmd, fileName, ppOptions, parsedArgs.containsKey("v"));
                else {
                    _programLines = Files.lines(Paths.get(fileName), StandardCharsets.UTF_8).collect(Collectors.toUnmodifiableList());
                    if (_programLines.stream().anyMatch(line -> ppp.matcher(line).find()))
                        _programLines = runFileThroughCPreprocessor(ppCmd, fileName, ppOptions, parsedArgs.containsKey("v"));
                }

                if (_programLines.isEmpty()) {
                    System.err.println("file '" + fileName + "': no text, could be preprocessor error");
                    System.exit(1);
                }
            }
            else {
                String libName = builtin_libs.get(iarg - fileArgs.size());
                fileName = "<builtin>:" + libName + ".c";

                var libRes = loader.getResourceAsStream("lib/" + libName + ".c");
                if (libRes == null) {
                    System.err.println("Cannot find library '" + libName + "'");
                    System.exit(1);
                }
                _programLines = new BufferedReader(
                        new InputStreamReader(libRes))
                            .lines().collect(Collectors.toUnmodifiableList());
            }

            programLinesCache.add(_programLines);
            final String programText = String.join("\n", _programLines);
            final int arg_no = iarg;
            final boolean report_warnings_in_libs = false;

            try {
                moduleEnv.setWarningHandler(arg_no, err -> {
                    int idx = err.pos.arg_no < 0 ? arg_no : err.pos.arg_no;
                    //noinspection ConstantConditions
                    if (err.is_error || idx < fileArgs.size() ||
                            (report_warnings_in_libs && warningTreatment[0] != WarningTreatment.IGNORE)) {
                        reportError(fileName, programLinesCache.get(idx), err);
                        if (err.is_error)
                            errors[0]++;
                        else
                            warnings[0]++;
                    }
                });
                ParseTree tree = buildParseTree(programText);
                ParseTreeVisitor v = new ParseTreeVisitor(moduleEnv);
                v.visit(tree);
                var requiredLibs = moduleEnv.requiredLibs();
                for (String libName: requiredLibs)
                    if (!builtin_libs.contains(libName)) {
                        builtin_libs.add(libName);
                        n_units++;
                    }
                if (errors[0] == 0 &&
                        (warnings[0] == 0 || warningTreatment[0] != WarningTreatment.TREAS_AS_ERRORS)
                        && iarg == n_units - 1) {
                    if (!requiredLibs.isEmpty()) {
                        System.err.println("Library function(s) were not found: " + moduleEnv.missingLibraryFuncs());
                        System.exit(1);
                    }
                    wat = moduleEnv.wat();
                }
            } catch (SyntaxError err) {
                // This should only be used for parsing errors
                reportError(fileName, programLinesCache.get(err.pos.arg_no < 0 ? arg_no : err.pos.arg_no), err);
                if (err.is_error)
                    errors[0]++;
                else
                    warnings[0]++;
            }
        }

        if (errors[0] > 0) {
            System.err.printf("\n%d warning%s and %d error%s generated.\n",
                    warnings[0], warnings[0] > 1? "s" : "",
                    errors[0], errors[0] > 1 ? "s" : "");
            System.exit(1);
        }
        else if (warnings[0] > 0 && warningTreatment[0] == WarningTreatment.TREAS_AS_ERRORS) {
            System.err.printf("\n%d warning%s generated (treated as errors due to '-Werror').\n",
                    warnings[0], warnings[0] > 1 ? "s" : "");
            System.exit(1);
        }
        else {
            assert wat != null;
            if (wat_output != null) {
                String sWat = wat.toStringPretty(2);
                if ("-".equals(wat_output))
                    System.out.println(wat);
                else
                    (new PrintStream(wat_output)).println(sWat);
            }
            if (wasm_output != null)
                wat.wasm(new WasmOutputStream(wasm_output));
        }
    }

    private static void reportError(String fileName, List<String> programLines, SyntaxError err) {
        /* Ideally, we should be using colors from GCC_COLORS; see
            https://gcc.gnu.org/onlinedocs/gcc-10.1.0/gcc/Diagnostic-Message-Formatting-Options.html
         */
        boolean is_tty = System.console() != null;
        final String sType = err.is_error ? (is_tty? Color.RED_BOLD + "error" + Color.RESET : "error") :
                                            (is_tty? (Color.MAGENTA_BOLD + "warning" + Color.RESET) : "warning");
        int lineno = err.pos.line_st;
        if (lineno < 0)
            System.err.printf("%s: %s\n", sType, err.msg);
        else {
            String realName = fileName;
            if (programLines.get(0).charAt(0) == '#') {
                var location = err.locate(programLines);
                realName = location.fileName;
                lineno = location.lineno;
            }

            System.err.printf("%s:%d:%d: %s: %s\n", realName, lineno, err.pos.pos_st, sType, err.msg);

            String errLine = programLines.get(err.pos.line_st - 1);
            System.err.println(errLine);
            int pos_en = err.pos.line_st == err.pos.line_en ? err.pos.pos_en : (errLine.length() - 1);
            System.err.println(" ".repeat(err.pos.pos_st) +
                            (is_tty? Color.GREEN_BRIGHT: "") +
                    "^".repeat(1 + pos_en - err.pos.pos_st) +
                    (is_tty ? Color.RESET : ""));

/*
            if (err.pos.line_st != err.pos.line_en)
                System.out.println("(Actual reported error location is " + (err.pos.line_en - err.pos.line_st + 1) + " lines long)");
*/
        }
    }

    // to be used from tests
    public static void runAndSave(String programText, boolean usePreprocessor, List<String> libs,
                                  @Nullable Path watFileName, SyntaxError.WarningInterface warnHandler) throws IOException {
        Properties prop = defaultProperties();

        List<String> ppOptions = new ArrayList<>();

        addPreprocessorSymbolsFromProperties(ppOptions, prop);

        if (usePreprocessor)
            programText = String.join("\n", runTextThroughCPreprocessor(programText, ppOptions));

        ModuleEnv moduleEnv = new ModuleEnv(prop);
        moduleEnv.setWarningHandler(-1, warnHandler);

        ParseTree tree = buildParseTree(programText);
        ParseTreeVisitor v = new ParseTreeVisitor(moduleEnv);
        v.visit(tree);
        List<String> copyLibs = new ArrayList<>(libs);
        copyLibs.addAll(moduleEnv.requiredLibs());

        for (int i = 0; i < copyLibs.size(); i ++) {
            String libName = copyLibs.get(i);
            moduleEnv.setWarningHandler(-1,null);
            var programLines = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    loader.getResourceAsStream("lib/" + libName + ".c"))))
                    .lines().collect(Collectors.toUnmodifiableList());
            programText = String.join("\n", programLines);

            tree = buildParseTree(programText);
            v = new ParseTreeVisitor(moduleEnv);
            v.visit(tree);
            for (String l : moduleEnv.requiredLibs())
                if (!copyLibs.contains(l))
                    copyLibs.add(l);
        }

        if (watFileName != null) {
            PrintStream out = new PrintStream(watFileName.toFile());
            out.println(moduleEnv.wat().toStringPretty(2));
        }
    }

    private static Properties defaultProperties () throws IOException {
        Properties prop = new Properties();
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
                " -D<name>=value  pass definition to C preprocessor\n" +
                " -X<name>=value  override compiler property (see below)\n" +
                " -l<name>        include built-in library <name> (use -lh to list available libraries)\n" +
                " -v              Print (on standard error output) the preprocessor commands\n" +
                " -k              If output is WASM, retain intermediary WAT file\n" +
                " -w              Inhibit all warning messages\n" +
                " -Werror         Make all warnings into errors\n" +
                " -o, --output <FILE>  specify output WAT file (or - for stdout)\n" +
//                " -k, --keep      when compiling to WASM, keep intermediate WAT file\n" +
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
                                               List<String> ppOptions, List<String> builtin_libs,
                                               WarningTreatment[] warningTreatment) throws IOException {
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
            else if (o.toLowerCase().startsWith("-w")) {
                if (o.equals("-w"))
                    warningTreatment[0] = WarningTreatment.IGNORE;
                else if (o.equals("-Werror"))
                    warningTreatment[0] = WarningTreatment.TREAS_AS_ERRORS;
                else if (o.equals("-Wall"))
                    ;
                else {
                    System.err.println("Invalid warning option '" + o + "'");
                    System.exit(1);
                }
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

    private static List<String> runFileThroughCPreprocessor(String ppCmd, String fileName, List<String> ppOptions, boolean verbose) throws IOException {
        // https://gcc.gnu.org/onlinedocs/gcc/Preprocessor-Options.html
        // -E invokes preprocessor
        // -P inhibits line markers [no longer needed]
        // -C preserves comments

        String include = getIncludePath();
        String command = ppCmd + " " + String.join(" ", ppOptions) + " -I" + include + " " + fileName;
        if (verbose)
            System.err.println(command);
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

        Properties prop = defaultProperties();
        String ppCmd = prop.getProperty("preprocessor.command");

        return runFileThroughCPreprocessor(ppCmd, tempFile.getAbsolutePath(), ppOptions, false);
    }

    static private class ThrowingErrorListener extends BaseErrorListener {
        // https://newbedev.com/handling-errors-in-antlr4
        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            //throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
            throw new SyntaxError(new SyntaxError.Position(line, charPositionInLine), msg, true);
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

    private static String getIncludePath() {
        URL testURL = Main.class.getClassLoader().getResource("gradle.properties");
        if (testURL == null)
            throw new RuntimeException("testURL = null");

        String protocol = testURL.getProtocol();

        if (protocol.equals("file")) {
            Path include = Paths.get(Paths.get(testURL.getPath()).getParent().toString(), "..", "..", "..", "src", "dist", "include").normalize();
            return include.toString();
        }

        else if(protocol.equals("jar")) {
            String jarPath = testURL.getPath().substring(5, testURL.getPath().indexOf("!")); //strip out only the JAR file
            Path include = Paths.get(Paths.get(jarPath).getParent().toString(), "..", "include").normalize();
            return include.toString();
        }

        else
            throw new RuntimeException("Unknown protocol " + protocol);
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

    // https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
    enum Color {
        //Color end string, color reset
        RESET("\033[0m"),

        // Regular Colors. Normal color, no bold, background color etc.
        BLACK("\033[0;30m"),    // BLACK
        RED("\033[0;31m"),      // RED
        GREEN("\033[0;32m"),    // GREEN
        YELLOW("\033[0;33m"),   // YELLOW
        BLUE("\033[0;34m"),     // BLUE
        MAGENTA("\033[0;35m"),  // MAGENTA
        CYAN("\033[0;36m"),     // CYAN
        WHITE("\033[0;37m"),    // WHITE

        // Bold
        BLACK_BOLD("\033[1;30m"),   // BLACK
        RED_BOLD("\033[1;31m"),     // RED
        GREEN_BOLD("\033[1;32m"),   // GREEN
        YELLOW_BOLD("\033[1;33m"),  // YELLOW
        BLUE_BOLD("\033[1;34m"),    // BLUE
        MAGENTA_BOLD("\033[1;35m"), // MAGENTA
        CYAN_BOLD("\033[1;36m"),    // CYAN
        WHITE_BOLD("\033[1;37m"),   // WHITE

        // Underline
        BLACK_UNDERLINED("\033[4;30m"),     // BLACK
        RED_UNDERLINED("\033[4;31m"),       // RED
        GREEN_UNDERLINED("\033[4;32m"),     // GREEN
        YELLOW_UNDERLINED("\033[4;33m"),    // YELLOW
        BLUE_UNDERLINED("\033[4;34m"),      // BLUE
        MAGENTA_UNDERLINED("\033[4;35m"),   // MAGENTA
        CYAN_UNDERLINED("\033[4;36m"),      // CYAN
        WHITE_UNDERLINED("\033[4;37m"),     // WHITE

        // Background
        BLACK_BACKGROUND("\033[40m"),   // BLACK
        RED_BACKGROUND("\033[41m"),     // RED
        GREEN_BACKGROUND("\033[42m"),   // GREEN
        YELLOW_BACKGROUND("\033[43m"),  // YELLOW
        BLUE_BACKGROUND("\033[44m"),    // BLUE
        MAGENTA_BACKGROUND("\033[45m"), // MAGENTA
        CYAN_BACKGROUND("\033[46m"),    // CYAN
        WHITE_BACKGROUND("\033[47m"),   // WHITE

        // High Intensity
        BLACK_BRIGHT("\033[0;90m"),     // BLACK
        RED_BRIGHT("\033[0;91m"),       // RED
        GREEN_BRIGHT("\033[0;92m"),     // GREEN
        YELLOW_BRIGHT("\033[0;93m"),    // YELLOW
        BLUE_BRIGHT("\033[0;94m"),      // BLUE
        MAGENTA_BRIGHT("\033[0;95m"),   // MAGENTA
        CYAN_BRIGHT("\033[0;96m"),      // CYAN
        WHITE_BRIGHT("\033[0;97m"),     // WHITE

        // Bold High Intensity
        BLACK_BOLD_BRIGHT("\033[1;90m"),    // BLACK
        RED_BOLD_BRIGHT("\033[1;91m"),      // RED
        GREEN_BOLD_BRIGHT("\033[1;92m"),    // GREEN
        YELLOW_BOLD_BRIGHT("\033[1;93m"),   // YELLOW
        BLUE_BOLD_BRIGHT("\033[1;94m"),     // BLUE
        MAGENTA_BOLD_BRIGHT("\033[1;95m"),  // MAGENTA
        CYAN_BOLD_BRIGHT("\033[1;96m"),     // CYAN
        WHITE_BOLD_BRIGHT("\033[1;97m"),    // WHITE

        // High Intensity backgrounds
        BLACK_BACKGROUND_BRIGHT("\033[0;100m"),     // BLACK
        RED_BACKGROUND_BRIGHT("\033[0;101m"),       // RED
        GREEN_BACKGROUND_BRIGHT("\033[0;102m"),     // GREEN
        YELLOW_BACKGROUND_BRIGHT("\033[0;103m"),    // YELLOW
        BLUE_BACKGROUND_BRIGHT("\033[0;104m"),      // BLUE
        MAGENTA_BACKGROUND_BRIGHT("\033[0;105m"),   // MAGENTA
        CYAN_BACKGROUND_BRIGHT("\033[0;106m"),      // CYAN
        WHITE_BACKGROUND_BRIGHT("\033[0;107m");     // WHITE

        private final String code;

        Color(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }
}
