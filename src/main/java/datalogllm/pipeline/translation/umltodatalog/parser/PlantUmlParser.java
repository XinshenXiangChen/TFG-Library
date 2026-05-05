package datalogllm.pipeline.translation.umltodatalog.parser;

import datalogllm.pipeline.umlMetamodel.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very lightweight parser that reads a subset of PlantUML class diagrams and
 * produces an {@link UmlModel}.
 */
public final class PlantUmlParser {

    private static final Pattern CLASS_DECL_PATTERN =
            Pattern.compile("^\\s*class\\s+(\\w+)\\s*\\{?\\s*$");
    private static final Pattern ATTRIBUTE_PATTERN_PREFIX =
            Pattern.compile("^\\s*[+#-]?\\s*(.+)$");
    private static final Pattern ATTRIBUTE_TYPE_NAME_PATTERN1 =
            Pattern.compile("^([\\w<>\\[\\]]+)\\s+(\\w+)$");
    private static final Pattern ATTRIBUTE_TYPE_NAME_PATTERN2 =
            Pattern.compile("^(\\w+)\\s*:\\s*([\\w<>\\[\\]]+)$");
    private static final Pattern GENERALIZATION_PATTERN =
            Pattern.compile("^\\s*(\\w+)\\s*<\\|--\\s*(\\w+)\\s*$");
    private static final Pattern ASSOCIATION_PATTERN =
            Pattern.compile("^\\s*(\\w+)(?:\\s+\"([^\"]+)\")?\\s*" +
                            "(-{1,2}|<{1,2}|>{1,2})?(-{1,2}|\\>{1,2}|<{1,2})\\s*" +
                            "\"?([^\"\\s]+)?\"?\\s*(\\w+)\\s*(?::\\s*(.*))?$");

    private PlantUmlParser() {
    }

    public static UmlModel parse(String plantUml) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");

        UmlModel model = new UmlModel();
        model.setNaturalLanguageConstraints(NaturalLanguageConstraintExtractor.extract(plantUml));
        Map<String, UmlClass> classesByName = new LinkedHashMap<>();
        List<Runnable> deferredRelations = new ArrayList<>();
        List<Runnable> deferredGeneralizations = new ArrayList<>();
        String[] lines = plantUml.split("\\R");
        UmlClass currentClass = null;
        int[] assocCounter = {1};

        for (String rawLine : lines) {
            String line = stripComments(rawLine).trim();
            if (line.isEmpty() || line.equalsIgnoreCase("@startuml") || line.equalsIgnoreCase("@enduml")) continue;
            if (currentClass != null && line.startsWith("}")) {
                currentClass = null;
                continue;
            }
            if (currentClass != null) {
                parseAttributeLine(line, currentClass);
                continue;
            }

            Matcher classMatcher = CLASS_DECL_PATTERN.matcher(line);
            if (classMatcher.matches()) {
                String className = classMatcher.group(1);
                currentClass = classesByName.computeIfAbsent(className, name -> {
                    UmlClass c = new UmlClass(name);
                    model.addClass(c);
                    return c;
                });
                continue;
            }

            Matcher genMatcher = GENERALIZATION_PATTERN.matcher(line);
            if (genMatcher.matches()) {
                String superName = genMatcher.group(1);
                String subName = genMatcher.group(2);
                deferredGeneralizations.add(() -> {
                    UmlClass superClass = getOrCreateClass(model, classesByName, superName);
                    UmlClass subClass = getOrCreateClass(model, classesByName, subName);
                    model.addGeneralization(new Generalization(subClass, superClass));
                });
                continue;
            }

            Matcher assocMatcher = ASSOCIATION_PATTERN.matcher(line);
            if (assocMatcher.matches()) {
                String leftClassName = assocMatcher.group(1);
                String leftMultText = assocMatcher.group(2);
                String leftArrowPart = assocMatcher.group(3);
                String rightArrowPart = assocMatcher.group(4);
                String rightMultText = assocMatcher.group(5);
                String rightClassName = assocMatcher.group(6);
                String label = assocMatcher.group(7);
                if (!isValidClassName(leftClassName) || !isValidClassName(rightClassName)) continue;

                deferredRelations.add(() -> {
                    UmlClass leftClass = getOrCreateClass(model, classesByName, leftClassName);
                    UmlClass rightClass = getOrCreateClass(model, classesByName, rightClassName);
                    String assocId = "A" + assocCounter[0]++;
                    String assocName = (label != null && !label.isBlank()) ? label.trim() : leftClass.getClassName() + "_" + rightClass.getClassName();
                    Association association = new Association(assocId, assocName);
                    model.addAssociation(association);
                    int[] leftBounds = parseMultiplicityBounds(leftMultText);
                    int[] rightBounds = parseMultiplicityBounds(rightMultText);
                    boolean arrowRight = (leftArrowPart != null && leftArrowPart.contains(">"))
                            || (rightArrowPart != null && rightArrowPart.contains(">"));
                    boolean leftNav = !arrowRight;
                    boolean rightNav = true;
                    if (!arrowRight) rightNav = true;
                    String leftRole = decapitalize(leftClass.getClassName());
                    String rightRole = decapitalize(rightClass.getClassName());

                    AssociationEnd leftEnd = new AssociationEnd(association, leftClass, leftRole, leftBounds[0], leftBounds[1], leftNav);
                    AssociationEnd rightEnd = new AssociationEnd(association, rightClass, rightRole, rightBounds[0], rightBounds[1], rightNav);
                    model.addAssociationEnd(leftEnd);
                    model.addAssociationEnd(rightEnd);
                });
            }
        }

        deferredRelations.forEach(Runnable::run);
        deferredGeneralizations.forEach(Runnable::run);
        return model;
    }

    private static String stripComments(String line) {
        int idx = line.indexOf('\'');
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    private static UmlClass getOrCreateClass(UmlModel model, Map<String, UmlClass> classesByName, String name) {
        return classesByName.computeIfAbsent(name, n -> {
            UmlClass c = new UmlClass(n);
            model.addClass(c);
            return c;
        });
    }

    private static void parseAttributeLine(String line, UmlClass currentClass) {
        if (line.startsWith("}")) return;
        Matcher prefixMatcher = ATTRIBUTE_PATTERN_PREFIX.matcher(line);
        if (!prefixMatcher.matches()) return;
        String content = prefixMatcher.group(1).trim();
        if (content.isEmpty()) return;
        Matcher m1 = ATTRIBUTE_TYPE_NAME_PATTERN1.matcher(content);
        if (m1.matches()) {
            currentClass.addAttribute(new UmlAttribute(currentClass, m1.group(2).trim(), m1.group(1).trim()));
            return;
        }
        Matcher m2 = ATTRIBUTE_TYPE_NAME_PATTERN2.matcher(content);
        if (m2.matches()) {
            currentClass.addAttribute(new UmlAttribute(currentClass, m2.group(1).trim(), m2.group(2).trim()));
        }
    }

    private static int[] parseMultiplicityBounds(String multText) {
        if (multText == null || multText.isBlank()) return new int[]{0, -1};
        multText = multText.trim();
        if (multText.equals("*")) return new int[]{0, -1};
        String[] parts = multText.split("\\.\\.");
        try {
            if (parts.length == 1) {
                int v = parseMultBound(parts[0]);
                return new int[]{v, v};
            }
            return new int[]{parseMultBound(parts[0]), parseMultBound(parts[1])};
        } catch (NumberFormatException ex) {
            return new int[]{0, -1};
        }
    }

    private static int parseMultBound(String s) {
        s = s.trim();
        if (s.equals("*") || s.equals("n")) return -1;
        return Integer.parseInt(s);
    }

    private static String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean isValidClassName(String name) {
        return name != null && name.length() >= 2 && Character.isUpperCase(name.charAt(0));
    }
}
