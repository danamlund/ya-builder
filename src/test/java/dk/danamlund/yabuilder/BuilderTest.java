package dk.danamlund.yabuilder;

import static dk.danamlund.yabuilder.AnnoProcTestTools.assertNoErrors;
import static dk.danamlund.yabuilder.AnnoProcTestTools.assertHasError;
import static dk.danamlund.yabuilder.AnnoProcTestTools.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
import org.junit.Test;
import org.junit.Assert;

// Referenced java files are in src/test/resources/
public class BuilderTest {
    @Test
    public void testBasic() throws Exception {
        assertNoErrors("Basic", "BasicBuilder.java");
    }

    @Test
    public void testUsageCompiles() throws Exception {
        assertNoErrors("Usage", "UsageBuilder.java", null);
    }

    @Test
    public void testStaticMethod() throws Exception {
        assertNoErrors("StaticMethod", "StaticMethodBuilder.java");
    }

    @Test
    public void testStaticMethodName() throws Exception {
        assertNoErrors("StaticMethodName", "StaticMethodNameMyBuilder.java");
    }

    @Test
    public void testBasicName() throws Exception {
        assertNoErrors("BasicName", "BasicNameMyBuilder.java");
    }

    @Test
    public void testAdvanced() throws Exception {
        assertNoErrors("Advanced", "AdvancedBuilder.java");
    }

    @Test
    public void testDefaultString() throws Exception {
        assertNoErrors("DefaultString", "DefaultStringBuilder.java");
    }

    @Test
    public void testVoidMethod() throws Exception {
        assertNoErrors("VoidMethod", "VoidMethod_mBuilder.java");
    }


    @Test
    public void testPrivateClass() throws Exception {
        assertHasError("PrivateClass", "@Builder methods parent classes must not be private");
    }

    @Test
    public void testPrivateParentClass() throws Exception {
        assertHasError("PrivateParentClass", 
                       "@Builder methods parent classes must not be private");
    }

    @Test
    public void testPrivateConstructor() throws Exception {
        assertHasError("PrivateConstructor", 
                       "has private access");
    }

    @Test
    public void testPrivateStaticMethod() throws Exception {
        assertHasError("PrivateStaticMethod", "must be static non-private");
    }

    @Test
    public void testNoParameters() throws Exception {
        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  @Builder",
                            "  C() {",
                            "  }",
                            "}"),
                       "@Builder method must have parameters");

        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  @Builder",
                            "  public static int m() {",
                            "    return 42;",
                            "  }",
                            "}"),
                       "@Builder method must have parameters");
    }

    @Test
    public void testRequiredAndDefault() throws Exception {
        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  private final int id;",
                            "  @Builder",
                            "  C(@Required @Default(\"42\") int id) {",
                            "    this.id = id;",
                            "  }",
                            "}"),
                       "@Default and @Required cannot both be on the same parameter");

        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  @Builder",
                            "  public static int C(@Required @Default(\"42\") int id) {",
                            "    return 42;",
                            "  }",
                            "}"),
                       "@Default and @Required cannot both be on the same parameter");
    }

    @Test
    public void testForgotBuilder() throws Exception {
        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  private final int id;",
                            "  C(@Required int id) {",
                            "    this.id = id;",
                            "  }",
                            "}"),
                       "@Required require that its method have @Builder");

        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  public static int C(@Required int id) {",
                            "    return 42;",
                            "  }",
                            "}"),
                       "@Required require that its method have @Builder");
    }

    @Test
    public void testNonStaticMethod() throws Exception {
        assertHasError(java("C",
                            "import dk.danamlund.yabuilder.*;",
                            "class C {",
                            "  public int C(@Required int id) {",
                            "    return 42;",
                            "  }",
                            "}"),
                       "@Required require that its method have @Builder");
    }
}
