package dk.danamlund.yabuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation Processor for {@link Builder}.
 *
 * @see dk.danamlund.yabuilder.Builder
 */
public final class BuilderProcessor extends AbstractProcessor {

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, 
                                                         AnnotationMirror annotation, 
                                                         ExecutableElement member, 
                                                         String userText) {
        return super.getCompletions(element, annotation, member, userText);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(Builder.class.getName(), 
                                           Required.class.getName(),
                                           Default.class.getName()));
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    private void error(String message, Element element) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void checkAnnotations(Element element,
                                  Class<? extends Annotation> hasAnnotation,
                                  Class<? extends Annotation> parentHasAnnotation,
                                  Class<? extends Annotation> hasNotAnnotation) {
        if (element.getEnclosingElement().getAnnotation(parentHasAnnotation) == null) {
            error("@" + hasAnnotation.getSimpleName() + " require that its method have @"
                  + parentHasAnnotation.getSimpleName(), element);
        }
        if (hasNotAnnotation != null && element.getAnnotation(hasNotAnnotation) != null) {
            error("@" + hasAnnotation.getSimpleName() + " and @" + hasNotAnnotation.getSimpleName()
                  + " cannot both be on the same parameter", element);
        }
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Default.class)) {
            checkAnnotations(e, Default.class, Builder.class, Required.class);
        }
        for (Element e : roundEnv.getElementsAnnotatedWith(Required.class)) {
            checkAnnotations(e, Required.class, Builder.class, Default.class);
        }

        OUTER: 
        for (Element e : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (e.getKind().equals(ElementKind.METHOD)) {
                if (e.getModifiers().contains(Modifier.PRIVATE)
                    || !e.getModifiers().contains(Modifier.STATIC)) {
                    error("@Builder method must be static non-private", e);
                    continue;
                }
            }

            for (Element p = e; p != null; p = p.getEnclosingElement()) {
                if (p.getKind() == ElementKind.CLASS
                    && p.getModifiers().contains(Modifier.PRIVATE)) {
                    error("@Builder methods parent classes must not be private", p);
                    continue OUTER;
                }
            }

            ExecutableElement ee = (ExecutableElement) e;

            final String eeName;
            final String eeReturnType;
            if (ee.getKind().equals(ElementKind.CONSTRUCTOR)) {
                eeName = String.valueOf(ee.getEnclosingElement().getSimpleName());
                eeReturnType = eeName;
            } else {
                eeName = getClassName(ee) + "." + String.valueOf(ee.getSimpleName());
                eeReturnType = String.valueOf(ee.getReturnType());
            }


            // Find parameters and default values
            Map<String, String> allParameters = new LinkedHashMap<>();
            Map<String, String> mandatorys = new LinkedHashMap<>();
            Map<String, String> optionals = new LinkedHashMap<>();
            Map<String, String> optionalsDefaults = new HashMap<>();
            for (Element parameter : ee.getParameters()) {
                if (ElementKind.PARAMETER.equals(parameter.getKind())) {
                    String pName = String.valueOf(parameter.getSimpleName());
                    String pType = String.valueOf(parameter.asType());
                    if (parameter.getAnnotation(Required.class) != null) {
                        mandatorys.put(pName, pType);
                    } else {
                        optionals.put(pName, pType);
                        Default defaultAnno = parameter.getAnnotation(Default.class);
                        if (defaultAnno != null) {
                            optionalsDefaults.put(pName, defaultAnno.value());
                        }
                    }
                    allParameters.put(pName, pType);
                }
            }

            if (allParameters.isEmpty()) {
                error("@Builder method must have parameters", e);
                continue;
            }

            // Construct Builder class
            try {
                String packageName = getPackage(ee);
                String className = getClassName(ee);

                Builder builderAnno = ee.getAnnotation(Builder.class);
                String builderName = builderAnno.value();
                if (builderName.isEmpty()) {
                    builderName = eeName.replace(".", "_") + "Builder";
                }
                String builderQualifiedName = (packageName.isEmpty() ? "" : packageName + ".") 
                    + builderName;
                JavaFileObject builderJava = processingEnv.getFiler()
                    .createSourceFile(builderQualifiedName);
                try (PrintWriter writer = new PrintWriter(builderJava.openWriter())) {
                        if (!packageName.isEmpty()) {
                            writer.println("package " + packageName + ";");
                        }
                        writer.println("");
                        writer.println("public class " + builderName
                                      + getSetterGenerics(mandatorys, null) + " {");

                        // Fields
                        for (String pName : allParameters.keySet()) {
                            String pType = allParameters.get(pName);

                            if (optionalsDefaults.containsKey(pName)) {
                                String defaultValue = optionalsDefaults.get(pName);
                                if (pType.equals("java.lang.String")) {
                                    defaultValue = '"' + defaultValue + '"';
                                }
                                writer.println("  private " + pType + " " 
                                              + pName + " = " + defaultValue +";");
                            } else {
                                writer.println("  private " + pType + " " 
                                              + pName + ";");
                            }
                        }
                        writer.println("");

                        // .build() method
                        writer.println("  public static " + eeReturnType +
                                      " build(java.util.function.Function<" +
                                      builderName + getNotSetGenerics(mandatorys) +
                                      ", " +
                                      builderName + getIsSetGenerics(mandatorys) +
                                      "> builder) {");
                        writer.println("    " + builderName + getIsSetGenerics(mandatorys) +
                                       " built = ");
                        writer.println("      builder.apply(" +
                                      "new " + builderName + getNotSetGenerics(mandatorys) +
                                      "());");
                        String eeQualifiedName = (packageName.isEmpty() ? "" : packageName + ".") 
                            + eeName;
                        if (ee.getKind().equals(ElementKind.CONSTRUCTOR)) {
                            writer.print("    return new " + eeQualifiedName + "(");
                        } else {
                            if (eeReturnType.equals("void")) {
                                writer.print("    " + eeQualifiedName + "(");
                            } else {
                                writer.print("    return " + eeQualifiedName + "(");
                            }
                        }
                        boolean first = true;
                        for (String pName : allParameters.keySet()) {
                            if (first) {
                                first = false;
                            } else {
                                writer.print(", ");
                            }
                            writer.print("built." + pName);
                        }
                        writer.println(");");
                        writer.println("  }");
                        writer.println("");

                        // Setter methods
                        for (String pName : allParameters.keySet()) {
                            String pType = allParameters.get(pName);
                            boolean isMandatory = mandatorys.containsKey(pName);

                            // javadoc
                            writer.println("  /**");
                            if (isMandatory) {
                                writer.println("   *  Required");
                            } else {
                                if (optionalsDefaults.containsKey(pName)) {
                                    String defaultValue = optionalsDefaults.get(pName);
                                    writer.println("   *  Optional (default: " + defaultValue + ")");
                                } else {
                                    writer.println("   *  Optional");
                                }
                            }
                            writer.println("   */");


                            // method
                            if (isMandatory) {
                                writer.println("  @SuppressWarnings(\"unchecked\")");
                            }
                            writer.println("  public " + builderName
                                          + getSetterGenerics(mandatorys, pName)
                                          + " " + pName + "(" 
                                          + pType + " " + pName + ") {");
                            writer.println("    this."+ pName + " = " + pName + ";");
                            if (isMandatory) {
                                writer.println("    return ("
                                              + builderName 
                                              + getSetterGenerics(mandatorys, pName)
                                              + ") this;");
                            } else {
                                writer.println("    return this;");
                            }
                            writer.println("  }");
                            writer.println("");
                        }

                        // Define helper classes
                        if (!mandatorys.isEmpty()) {
                            writer.println("  public static class Good { }");
                            for (String pName : mandatorys.keySet()) {
                                writer.println("  public static class "
                                              + getMissingGeneric(pName)+" { }");
                            }
                        }

                        writer.println("}");
                    }
            }
            catch (IOException ex) {
                error(ex.getMessage(), null);
            }
        }

        return true;
    }

    private static String getPackage(Element e) {
        while (!(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return String.valueOf(((PackageElement) e).getQualifiedName());
    }

    private static String getClassName(Element e) {
        while (!(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        String qName = String.valueOf(((TypeElement) e).getQualifiedName());
        String pName = getPackage(e);
        if (!pName.isEmpty()) {
            return qName.substring(pName.length());
        } else {
            return qName;
        }
    }

    private static String getMissingGeneric(String pName) {
        pName = pName.substring(0, 1).toUpperCase() + pName.substring(1);
        return "Missing" + pName;
    }

    private static String getGenerics(Map<String, String> parameters, 
                                      Function<String, String> mapper) {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        for (String pName : parameters.keySet()) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(mapper.apply(pName));
        }
        sb.append(">");
        return sb.toString();
    }

    private static String getSetterGenerics(Map<String, String> parameters, String parameterSetter) {
        return getGenerics(parameters, 
                           name -> (name.equals(parameterSetter) ? "Good" : name.toUpperCase()));
    }

    private static String getNotSetGenerics(Map<String, String> parameters) {
        return getGenerics(parameters, name -> getMissingGeneric(name));
    }

    private static String getIsSetGenerics(Map<String, String> parameters) {
        return getGenerics(parameters, name -> "Good");
    }
}
