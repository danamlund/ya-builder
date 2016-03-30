package dk.danamlund.yabuilder;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.Assert;
import org.junit.Test;

public class AnnoProcTestTools {

    public static void assertNoErrors(String className, String generatedName) throws Exception {
        assertNoErrors(className, generatedName, className);
    }

    public static void assertNoErrors(String className, String generatedName, String runClass) 
        throws Exception {
        try (Compiler compiler = new Compiler();) {
                String error = compiler.compile(runClass, resourceJava(className + ".java"));
                if (!error.isEmpty()) {
                    System.err.println("## " + generatedName + ":");
                    System.err.println(compiler.getSource(generatedName));
                    Assert.assertEquals("", error);
                }
            }
    }

    public static void assertHasError(String className, String errorContains) throws Exception {
        try (Compiler compiler = new Compiler();) {
                String error = compiler.compile(null, resourceJava(className + ".java"));
                Assert.assertTrue("("+error+") did not contain ("+errorContains+")", 
                                  error.contains(errorContains));
            }
    }

    public static void assertHasError(JavaFileObject java, String errorContains) throws Exception {
        try (Compiler compiler = new Compiler();) {
                String error = compiler.compile(null, java);
                Assert.assertTrue("("+error+") did not contain ("+errorContains+")", 
                                  error.contains(errorContains));
            }
    }


    public static JavaFileObject java(String name, String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append("\n");
        }
        return new JavaSourceFromString(name, sb.toString());
    }

    public static JavaFileObject resourceJava(String name) {
        String text = new Scanner(AnnoProcTestTools.class.getClassLoader()
                                  .getResourceAsStream(name), "UTF-8")
            .useDelimiter("\\A").next();
        return new JavaSourceFromString(name.substring(0, name.length() - ".java".length()), text);
    }

    public static class Compiler implements Closeable {
        private final File tempDir;
        
        public Compiler() throws Exception {
            tempDir = Files.createTempDirectory("BuilderTest").toFile();
        }

        public String compile(String runClass, JavaFileObject... javas) throws Exception {
            List<JavaFileObject> javaObjects = Arrays.asList(javas);
            List<String> javaNames = javaObjects.stream().map(o -> name(o))
                .collect(Collectors.toList());
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tempDir));
            CompilationTask task = compiler.getTask(new PrintWriter(System.out), fileManager, diagnostics, 
                                                    null, javaNames, javaObjects);
            task.setProcessors(Arrays.asList(new BuilderProcessor()));
            String errors = "";
            if (task.call()) {
                if (runClass != null) {
                    ClassLoader cl = fileManager.getClassLoader(StandardLocation.CLASS_OUTPUT);
                    Class<?> loadedClass = cl.loadClass(runClass);
                    Method testMethod = loadedClass.getMethod("test");
                    testMethod.setAccessible(true);
                    testMethod.invoke(null);
                }
            } else {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String className = name(diagnostic.getSource());
                    errors += className + ":" + diagnostic.getLineNumber() 
                        + ": " + diagnostic.getMessage(Locale.US);
                }
            }
            fileManager.close();
            return errors;
        }

        public String getSource(String file) throws IOException {
            try {
            return Files.lines(tempDir.toPath().resolve(file))
                .collect(Collectors.joining(String.format("%n")));
            } catch (IOException e) {
                throw new IOException(Files.list(tempDir.toPath())
                                      .map(Object::toString)
                                      .collect(Collectors.joining(", ")));
            }
        }

        @Override
        public void close() {
            try {
                delete(tempDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static String name(JavaFileObject o) {
            if (o == null) {
                return null;
            }
            String className = o.getName();
            className = className.substring(1, className.length() - ".java".length());
            return className;
        }

        private static void delete(File f) throws IOException {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    delete(c);
                }
            }
            if (!f.delete()) {
                throw new FileNotFoundException("Failed to delete file: " + f);
            }
        }
    }
}
