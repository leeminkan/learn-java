package com.leeminkan.lombok;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MyBuilderProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(MyBuilder.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return false;

        for (Element element : roundEnv.getElementsAnnotatedWith(MyBuilder.class)) {

            // 1. Check if the annotated element is actually a CLASS
            if (element.getKind() != javax.lang.model.element.ElementKind.CLASS) {

                // This stops the build!
                processingEnv.getMessager().printMessage(
                        javax.tools.Diagnostic.Kind.ERROR,
                        "@MyBuilder can only be applied to classes. You applied it to: " + element.getKind(),
                        element // Passing the 'element' here underlines the specific code in the IDE
                );

                continue; // Skip this element and move to the next
            }

            // 2. Check if the class is abstract
            TypeElement classElement = (TypeElement) element;
            if (classElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
                processingEnv.getMessager().printMessage(
                        javax.tools.Diagnostic.Kind.ERROR,
                        "Cannot apply @MyBuilder to an abstract class.",
                        element
                );
                continue;
            }

            try {
                writeBuilderFile(classElement);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private void writeBuilderFile(TypeElement classElement) throws Exception {
        String className = classElement.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).toString();
        String builderName = className + "Builder";

        List<VariableElement> fields = classElement.getEnclosedElements().stream()
                .filter(e -> e.getKind().isField())
                .map(e -> (VariableElement) e)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("public class ").append(builderName).append(" {\n");

        // 1. Fields
        for (VariableElement field : fields) {
            sb.append("    private ").append(field.asType()).append(" ").append(field.getSimpleName()).append(";\n");
        }

        // 2. Setters
        for (VariableElement field : fields) {
            String name = field.getSimpleName().toString();
            String type = field.asType().toString();
            sb.append("    public ").append(builderName).append(" ").append(name).append("(").append(type).append(" value) {\n");
            sb.append("        this.").append(name).append(" = value;\n");
            sb.append("        return this;\n");
            sb.append("    }\n");
        }

        // 3. The Build Method
        sb.append("    public ").append(className).append(" build() {\n");
        sb.append("        return new ").append(className).append("(");

        // Join field names for the constructor call: e.g., "id, name, email"
        String constructorArgs = fields.stream()
                .map(f -> "this." + f.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        sb.append(constructorArgs).append(");\n");
        sb.append("    }\n");
        sb.append("}\n");

        // Write file...
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + builderName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.print(sb.toString());
        }
    }
}