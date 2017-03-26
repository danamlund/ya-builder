package dk.danamlund.yabuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
                                  List<Class<? extends Annotation>> hasNotAnnotations) {
        if (element.getEnclosingElement().getAnnotation(parentHasAnnotation) == null) {
            error("@" + hasAnnotation.getSimpleName() + " require that its method have @"
                  + parentHasAnnotation.getSimpleName(), element);
        }
        if (!hasNotAnnotations.isEmpty()) {
            for (Class<? extends Annotation> hasNotAnnotation : hasNotAnnotations) {
                if (element.getAnnotation(hasNotAnnotation) != null) {
                    error("@" + hasAnnotation.getSimpleName() + " and @"
                          + hasNotAnnotation.getSimpleName()
                          + " cannot both be on the same parameter", element);
                }
            }
        }
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Default.class)) {
            checkAnnotations(e, Default.class, Builder.class,
                             Arrays.asList(Required.class, RequiredOneOf.class));
        }
        for (Element e : roundEnv.getElementsAnnotatedWith(Required.class)) {
            checkAnnotations(e, Required.class, Builder.class,
                             Arrays.asList(Default.class, RequiredOneOf.class));
        }
        for (Element e : roundEnv.getElementsAnnotatedWith(RequiredOneOf.class)) {
            checkAnnotations(e, RequiredOneOf.class, Builder.class,
                             Arrays.asList(Default.class, Required.class));
        }

        try {
            for (Element e : roundEnv.getElementsAnnotatedWith(Builder.class)) {
                if (e.getKind().equals(ElementKind.METHOD)) {
                    if (e.getModifiers().contains(Modifier.PRIVATE)
                        || !e.getModifiers().contains(Modifier.STATIC)) {
                        throw new BuilderException(e, "@Builder method must be static non-private");
                    }
                }

                for (Element p = e; p != null; p = p.getEnclosingElement()) {
                    if (p.getKind() == ElementKind.CLASS
                        && p.getModifiers().contains(Modifier.PRIVATE)) {
                        throw new BuilderException(e, "@Builder methods parent classes " +
                                                   "must not be private");
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
                BuilderParametersHelper param = new BuilderParametersHelper(e);
            
                for (Element parameter : ee.getParameters()) {
                    if (ElementKind.PARAMETER.equals(parameter.getKind())) {
                        String pName = String.valueOf(parameter.getSimpleName());
                        String pType = String.valueOf(parameter.asType());
                        if (parameter.getAnnotation(Required.class) != null) {
                            param.addParameter(pName, pType);
                        } else if (parameter.getAnnotation(RequiredOneOf.class) != null) {
                            RequiredOneOf requiredOneOff = parameter.getAnnotation(RequiredOneOf.class);
                            param.addParameter(pName, pType, requiredOneOff);
                        } else {
                            Default defaultAnno = parameter.getAnnotation(Default.class);
                            param.addParameter(pName, pType, defaultAnno);
                        }
                    }
                }

                param.finishedAddingParameters();
            
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
                                       + param.getSetterGenerics() + " {");

                        // Fields
                        for (String pName : param.getParams()) {
                            String pType = param.getType(pName);

                            if (param.hasDefault(pName)) {
                                String defaultValue = param.getDefault(pName);
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
                                       builderName + param.getNotSetGenerics() +
                                       ", " +
                                       builderName + param.getIsSetGenerics() +
                                       "> builder) {");
                        writer.println("    " + builderName + param.getIsSetGenerics() +
                                       " built = ");
                        writer.println("      builder.apply(" +
                                       "new " + builderName + param.getNotSetGenerics() +
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
                        for (String pName : param.getParams()) {
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
                        for (String pName : param.getParams()) {
                            String pType = param.getType(pName);

                            // javadoc
                            writer.println("  /**");
                            if (param.isMandatory(pName)) {
                                if (param.isRequiredOneOf(pName)) {
                                    writer.println("   *  Requires one of: " +
                                                   String.join(", ",
                                                               param.getParamsInSameGroupAs(pName))
                                                   + ".");
                                } else {
                                    writer.println("   *  Required.");
                                }
                            } else {
                                if (param.hasDefault(pName)) {
                                    String defaultValue = param.getDefault(pName);
                                    writer.println("   *  Optional (default: " + defaultValue + ").");
                                } else {
                                    writer.println("   *  Optional.");
                                }
                            }
                            writer.println("   */");


                            // method
                            if (param.isMandatory(pName)) {
                                writer.println("  @SuppressWarnings(\"unchecked\")");
                            }
                            writer.println("  public " + builderName
                                           + param.getSetterGenerics(pName)
                                           + " " + pName + "(" 
                                           + pType + " " + pName + ") {");
                            writer.println("    this."+ pName + " = " + pName + ";");
                            if (param.isMandatory(pName)) {
                                writer.println("    return ("
                                               + builderName 
                                               + param.getSetterGenerics(pName)
                                               + ") this;");
                            } else {
                                writer.println("    return this;");
                            }
                            writer.println("  }");
                            writer.println("");
                        }

                        // Define helper classes
                        if (param.hasMandatorys()) {
                            writer.println("  public static class Good { }");
                            for (String missingGeneric : param.getNotSetGenericsNames()) {
                                writer.println("  public static class "
                                               + missingGeneric + " { }");
                            }
                        }

                        writer.println("}");
                    }
                }
                catch (IOException ex) {
                    error(ex.getMessage(), null);
                }
            }
        } catch (BuilderException e) {
            error(e.getMessage(), e.e);
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

    private static class BuilderParametersHelper {
        final Element e;
        Map<String, String> namesToType = new LinkedHashMap<>();
        Map<String, String> namesToDefault = new HashMap<>();
        Map<String, String> namesToGroup = new HashMap<>();
        Map<String, List<String>> groupToNames = new HashMap<>();
        Set<String> mandatorys = new HashSet<>();

        public BuilderParametersHelper(Element e) {
            this.e = e;
        }

        void addParameter(String name, String type) {
            namesToType.put(name, type);
            mandatorys.add(name);
        }
        void addParameter(String name, String type, RequiredOneOf requiredOneOfAnno) {
            namesToType.put(name, type);
            namesToGroup.put(name, requiredOneOfAnno.value());
        }
        void addParameter(String name, String type, Default defaultAnno) {
            namesToType.put(name, type);
            if (defaultAnno != null && defaultAnno.value() != null) {
                namesToDefault.put(name, defaultAnno.value());
            }
        }
        void finishedAddingParameters() throws BuilderException {
            for (Map.Entry<String, String> entry : namesToGroup.entrySet()) {
                String name = entry.getKey();
                String group = entry.getValue();
                List<String> groupNames = groupToNames.get(group);
                if (groupNames == null) {
                    groupNames = new ArrayList<>();
                    groupToNames.put(group, groupNames);
                }
                groupNames.add(name);
            }
            
            for (Map.Entry<String, List<String>> entry : groupToNames.entrySet()) {
                String group = entry.getKey();
                List<String> groupNames = entry.getValue();
                if (groupNames.size() <= 1) {
                    throw new BuilderException(e, "@RequiredOneOf(\""  + group +
                                               "\") must be on at least two parameters.");
                }
            }
            

            if (namesToType.isEmpty()) {
                throw new BuilderException(e, "@Builder method must have parameters");
            }
        }
        
        
        List<String> getParams() {
            return new ArrayList<>(namesToType.keySet());
        }

        String getType(String name) {
            return namesToType.get(name);
        }

        boolean isRequiredOneOf(String name) {
            return namesToGroup.containsKey(name);
        }

        List<String> getParamsInSameGroupAs(String name) {
            return groupToNames.get(namesToGroup.get(name));
        }

        String getSetterGenerics() {
            return getSetterGenerics(null);
        }
        
        String getSetterGenerics(String paramSetter) {
            String notSetGenericSetter = paramSetter == null ? null : getNotSetGeneric(paramSetter);
            return getGenerics(notSetGeneric -> (notSetGeneric.equals(notSetGenericSetter)
                                                 ? "Good" : notSetGeneric.toUpperCase()));
        }

        String getNotSetGenerics() {
            return getGenerics(Function.identity());
        }

        String getIsSetGenerics() {
            return getGenerics(name -> "Good");
        }

        boolean hasDefault(String name) {
            return namesToDefault.containsKey(name);
        }

        String getDefault(String name) {
            return namesToDefault.get(name);
        }

        boolean isMandatory(String name) {
            return mandatorys.contains(name) || namesToGroup.containsKey(name);
        }

        boolean hasMandatorys() {
            return !mandatorys.isEmpty() || !groupToNames.isEmpty();
        }

        List<String> getNotSetGenericsNames() {
            List<String> notSetGenerics = new ArrayList<>();
            Set<String> seenGroups = new HashSet<>();
            for (String name : getParams()) {
                if (isMandatory(name)) {
                    if (isRequiredOneOf(name)) {
                        if (!seenGroups.contains(namesToGroup.get(name))) {
                            notSetGenerics.add(getNotSetGeneric(name));
                            seenGroups.add(namesToGroup.get(name));
                        }
                    } else if (isMandatory(name)) {
                        notSetGenerics.add(getNotSetGeneric(name));
                    }
                }
            }
            return notSetGenerics;
        }

        private String getNotSetGeneric(String name) {
            if (isRequiredOneOf(name)) {
                return "MissingOneOf" + upcaseWord(namesToGroup.get(name));
            } else {
                return "Missing" + upcaseWord(name);
            }
        }

        private static String upcaseWord(String s) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }

        private String getGenerics(Function<String, String> mapper) {
            List<String> notSetGenerics = getNotSetGenericsNames();
            if (notSetGenerics.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<");
            for (String notSetGeneric : notSetGenerics) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(mapper.apply(notSetGeneric));
            }
            sb.append(">");
            return sb.toString();
        }
    }

    private static class BuilderException extends Exception {
        final Element e;
        BuilderException(Element e, String msg) {
            super(msg);
            this.e = e;
        }
    }
}
