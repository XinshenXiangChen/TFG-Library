package datalogllm.pipeline.umlMetamodel.services.java;

import datalogllm.pipeline.umlMetamodel.Association;
import datalogllm.pipeline.umlMetamodel.AssociationEnd;
import datalogllm.pipeline.umlMetamodel.Generalization;
import datalogllm.pipeline.umlMetamodel.UmlAttribute;
import datalogllm.pipeline.umlMetamodel.UmlClass;
import datalogllm.pipeline.umlMetamodel.UmlModel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class UmlModelToJavaGenerator {
    private static final Set<String> JAVA_KEYWORDS = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "record", "var", "yield", "true", "false", "null"
    );

    private static final Map<String, String> TYPE_MAP = new LinkedHashMap<>();

    static {
        TYPE_MAP.put("string", "String");
        TYPE_MAP.put("int", "int");
        TYPE_MAP.put("integer", "Integer");
        TYPE_MAP.put("long", "long");
        TYPE_MAP.put("boolean", "boolean");
        TYPE_MAP.put("bool", "Boolean");
        TYPE_MAP.put("double", "double");
        TYPE_MAP.put("float", "float");
        TYPE_MAP.put("date", "java.time.LocalDate");
        TYPE_MAP.put("datetime", "java.time.LocalDateTime");
        TYPE_MAP.put("timestamp", "java.time.Instant");
    }

    private final String targetPackage;
    private final Path outputDirectory;

    UmlModelToJavaGenerator(String targetPackage, Path outputDirectory) {
        this.targetPackage = targetPackage == null ? "" : targetPackage.trim();
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }

    void generate(UmlModel model) {
        Objects.requireNonNull(model, "model must not be null");
        Path base = outputDirectory;
        if (!targetPackage.isEmpty()) {
            base = outputDirectory.resolve(targetPackage.replace('.', '/'));
        }
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create output directory: " + base, e);
        }

        Map<String, String> subToSuper = inheritanceBySubclass(model);
        Map<String, List<FieldDef>> associationFields = associationFieldsByClass(model);

        for (UmlClass umlClass : model.getClasses()) {
            String className = umlClass.getClassName();
            if (!isValidJavaClassName(className)) continue;
            String source = generateClass(umlClass, subToSuper.get(className), associationFields.get(className));
            Path file = base.resolve(safeClassName(className) + ".java");
            try {
                Files.writeString(file, source, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write " + file, e);
            }
        }
    }

    private String generateClass(UmlClass umlClass, String superClass, List<FieldDef> associationFields) {
        StringBuilder sb = new StringBuilder();
        if (!targetPackage.isEmpty()) sb.append("package ").append(targetPackage).append(";\n\n");
        if (hasListType(associationFields)) sb.append("import java.util.List;\n\n");

        String extendsClause = "";
        if (superClass != null && !superClass.isBlank()) extendsClause = " extends " + safeClassName(superClass);

        sb.append("/**\n * Generated from UML metamodel class ").append(umlClass.getClassName()).append(".\n */\n");
        sb.append("public class ").append(safeClassName(umlClass.getClassName())).append(extendsClause).append(" {\n\n");

        for (UmlAttribute attribute : umlClass.getAttributes()) {
            sb.append("    private ").append(toJavaType(attribute.getType())).append(" ")
                .append(safeFieldName(attribute.getName())).append(";\n");
        }
        if (associationFields != null) {
            for (FieldDef associationField : associationFields) {
                sb.append("    private ").append(toJavaType(associationField.type)).append(" ")
                    .append(safeFieldName(associationField.name)).append(";\n");
            }
        }
        sb.append("\n");

        for (UmlAttribute attribute : umlClass.getAttributes()) {
            appendGetterSetter(sb, attribute.getName(), toJavaType(attribute.getType()));
        }
        if (associationFields != null) {
            for (FieldDef associationField : associationFields) {
                appendGetterSetter(sb, associationField.name, toJavaType(associationField.type));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static Map<String, String> inheritanceBySubclass(UmlModel model) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Generalization generalization : model.getGeneralizations()) {
            out.put(generalization.getSubClass().getClassName(), generalization.getSuperClass().getClassName());
        }
        return out;
    }

    private static Map<String, List<FieldDef>> associationFieldsByClass(UmlModel model) {
        Map<String, List<FieldDef>> out = new LinkedHashMap<>();
        for (Association association : model.getAssociations()) {
            List<AssociationEnd> ends = association.getEnds();
            if (ends.size() != 2) continue;
            AssociationEnd e0 = ends.get(0);
            AssociationEnd e1 = ends.get(1);
            if (!e0.isNavigable() && !e1.isNavigable()) continue;

            if (e1.isNavigable()) {
                boolean multiple = e1.getMax() == -1 || e1.getMax() > 1;
                String fieldName = toFieldName(e1.getRoleName(), multiple);
                String typeName = multiple ? "List<" + e1.getType().getClassName() + ">" : e1.getType().getClassName();
                out.computeIfAbsent(e0.getType().getClassName(), ignored -> new ArrayList<>()).add(new FieldDef(fieldName, typeName));
            }
            if (e0.isNavigable()) {
                boolean multiple = e0.getMax() == -1 || e0.getMax() > 1;
                String fieldName = toFieldName(e0.getRoleName(), multiple);
                String typeName = multiple ? "List<" + e0.getType().getClassName() + ">" : e0.getType().getClassName();
                out.computeIfAbsent(e1.getType().getClassName(), ignored -> new ArrayList<>()).add(new FieldDef(fieldName, typeName));
            }
        }
        return out;
    }

    private static String toFieldName(String roleName, boolean multiple) {
        if (roleName == null || roleName.isBlank()) return "items";
        String s = roleName.trim();
        if (multiple && !s.endsWith("s") && !s.endsWith("List")) s = s + "s";
        return Character.toLowerCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
    }

    private static boolean hasListType(List<FieldDef> fields) {
        if (fields == null) return false;
        for (FieldDef field : fields) if (field.type.startsWith("List<")) return true;
        return false;
    }

    private static void appendGetterSetter(StringBuilder sb, String rawName, String type) {
        String name = safeFieldName(rawName);
        String cap = name.substring(0, 1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
        sb.append("    public ").append(type).append(" get").append(cap).append("() {\n");
        sb.append("        return ").append(name).append(";\n");
        sb.append("    }\n\n");
        sb.append("    public void set").append(cap).append("(").append(type).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
        sb.append("    }\n\n");
    }

    private static boolean isValidJavaClassName(String name) {
        return name != null && name.length() >= 2 && Character.isUpperCase(name.charAt(0));
    }

    private static String toJavaType(String type) {
        if (type == null || type.isBlank()) return "String";
        String key = type.trim().toLowerCase();
        String mapped = TYPE_MAP.getOrDefault(key, type.trim());
        if (mapped.startsWith("List<") && mapped.endsWith(">")) {
            String inner = mapped.substring(5, mapped.length() - 1).trim();
            return "List<" + safeClassName(inner) + ">";
        }
        return safeClassName(mapped);
    }

    private static String safeClassName(String name) {
        if (name == null || name.isBlank()) return "Unknown";
        if (JAVA_KEYWORDS.contains(name.toLowerCase())) return name + "_";
        return name;
    }

    private static String safeFieldName(String name) {
        if (name == null || name.isBlank()) return "value";
        if (JAVA_KEYWORDS.contains(name.toLowerCase())) return name + "_";
        return name;
    }

    private record FieldDef(String name, String type) {}
}
