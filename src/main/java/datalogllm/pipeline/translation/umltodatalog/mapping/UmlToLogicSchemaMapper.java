package datalogllm.pipeline.translation.umltodatalog.mapping;

import datalogllm.pipeline.translation.umltodatalog.utils.UmlToDatalogUtils;
import datalogllm.pipeline.umlMetamodel.*;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.LogicSchema;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class UmlToLogicSchemaMapper {
    private static final Set<String> SQL_KEYWORDS = Set.of(
        "date", "time", "timestamp", "year", "month", "day",
        "select", "from", "where", "group", "order", "by",
        "table", "create", "drop", "insert", "update", "delete",
        "primary", "key", "constraint", "assertion", "join",
        "user", "role", "index", "view"
    );

    public record Inheritance(UmlClass subClass, UmlClass superClass) { }

    public String toDatalog(UmlModel umlModel) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        List<Inheritance> inheritances = umlModel.getGeneralizations().stream()
            .map(g -> new Inheritance(g.getSubClass(), g.getSuperClass()))
            .toList();
        return toDatalog(umlModel, inheritances);
    }

    public String toDatalog(UmlModel umlModel, List<Inheritance> inheritances) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        Objects.requireNonNull(inheritances, "inheritances must not be null");
        LogicSchema schema = UmlModelToLogicSchema.fromUmlModel(umlModel, inheritances);
        return UmlToDatalogUtils.printLogicSchema(schema);
    }

    /**
     * Builds an IMP-Logics {@link LogicSchema} in memory (no Datalog text round-trip).
     * {@link #toDatalog(UmlModel, List)} serializes this schema with {@link UmlToDatalogUtils#printLogicSchema(LogicSchema)}.
     */
    public LogicSchema toLogicSchema(UmlModel umlModel) {
        return UmlModelToLogicSchema.fromUmlModel(umlModel);
    }

    /**
     * Same as {@link #toLogicSchema(UmlModel)} with an explicit inheritance list
     * (e.g. when generalizations are not all stored on {@code umlModel}).
     */
    public LogicSchema toLogicSchema(UmlModel umlModel, List<Inheritance> inheritances) {
        return UmlModelToLogicSchema.fromUmlModel(umlModel, inheritances);
    }

    public JSONObject toJsonSchema(UmlModel umlModel) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        JSONObject root = new JSONObject();
        Map<String, String> subToSuper = new LinkedHashMap<>();
        for (Generalization g : umlModel.getGeneralizations()) {
            subToSuper.put(g.getSubClass().getClassName(), g.getSuperClass().getClassName());
        }
        Map<String, List<String>> associationFieldsByClass = associationFieldsByClass(umlModel);

        for (UmlClass c : umlModel.getClasses()) {
            JSONObject classObj = new JSONObject();
            for (UmlAttribute a : c.getAttributes()) classObj.put(a.getName(), a.getType());
            String pk = inferPrimaryKey(c);
            if (pk != null) classObj.put("primary key", pk);
            String superClass = subToSuper.get(c.getClassName());
            if (superClass != null && !superClass.isBlank()) classObj.put("extends", superClass);

            List<String> assocFields = associationFieldsByClass.get(c.getClassName());
            if (assocFields != null && !assocFields.isEmpty()) {
                JSONObject associations = new JSONObject();
                for (String f : assocFields) {
                    String[] parts = f.split(":", 2);
                    associations.put(parts[0], parts[1]);
                }
                classObj.put("associations", associations);
            }
            root.put(c.getClassName(), classObj);
        }

        JSONArray associationTables = new JSONArray();
        for (Association association : umlModel.getAssociations()) {
            List<AssociationEnd> ends = association.getEnds();
            if (ends.size() != 2) continue;
            AssociationEnd leftEnd = ends.get(0);
            AssociationEnd rightEnd = ends.get(1);
            String leftColumn = idColumnName(leftEnd.getType(), "leftId");
            String rightColumn = idColumnName(rightEnd.getType(), "rightId");
            JSONObject table = new JSONObject();
            table.put("name", association.getName());
            table.put("leftColumn", leftColumn);
            table.put("rightColumn", rightColumn);
            associationTables.put(table);
        }
        root.put("__associationTables", associationTables);
        return root;
    }

    public String toSqlSchema(UmlModel umlModel) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        StringBuilder sb = new StringBuilder();

        for (UmlClass c : umlModel.getClasses()) {
            if (c.getAttributes().isEmpty()) continue;
            sb.append("CREATE TABLE ").append(sqlIdentifier(c.getClassName())).append(" (\n");
            List<String> columnDefs = new ArrayList<>();
            for (UmlAttribute a : c.getAttributes()) {
                String sqlType = mapUmlTypeToSql(a.getType());
                columnDefs.add("    " + sqlIdentifier(a.getName()) + " " + sqlType);
            }
            String pk = inferPrimaryKey(c);
            if (pk != null) columnDefs.add("    PRIMARY KEY (" + sqlIdentifier(pk) + ")");
            sb.append(String.join(",\n", columnDefs));
            sb.append("\n);\n\n");
        }

        for (Association association : umlModel.getAssociations()) {
            List<AssociationEnd> ends = association.getEnds();
            if (ends.size() != 2) continue;
            AssociationEnd leftEnd = ends.get(0);
            AssociationEnd rightEnd = ends.get(1);
            String leftColumn = idColumnName(leftEnd.getType(), "leftId");
            String rightColumn = idColumnName(rightEnd.getType(), "rightId");

            List<String> columnDefs = new ArrayList<>();
            columnDefs.add("    " + sqlColumnDefinition(leftEnd.getType(), leftColumn));
            foreignKeyClause(leftEnd, leftColumn).ifPresent(columnDefs::add);
            columnDefs.add("    " + sqlColumnDefinition(rightEnd.getType(), rightColumn));
            foreignKeyClause(rightEnd, rightColumn).ifPresent(columnDefs::add);

            sb.append("CREATE TABLE ")
                .append(sqlIdentifier(association.getName()))
                .append(" (\n")
                .append(String.join(",\n", columnDefs))
                .append("\n);\n\n");
        }
        return sb.toString();
    }

    /**
     * Emits {@code FOREIGN KEY} when this association end has maximum multiplicity 1
     * (e.g. PlantUML {@code "1"} or {@code "0..1"}): the column references the
     * linked class table on its primary key, or its first attribute if no PK is inferred.
     */
    private static Optional<String> foreignKeyClause(AssociationEnd end, String localColumn) {
        if (end.getMax() != 1) {
            return Optional.empty();
        }
        UmlClass referencedClass = end.getType();
        return referencableColumn(referencedClass).map(refColumn ->
                "    FOREIGN KEY (" + sqlIdentifier(localColumn) + ") REFERENCES "
                        + sqlIdentifier(referencedClass.getClassName()) + "(" + sqlIdentifier(refColumn) + ")");
    }

    private static Optional<String> referencableColumn(UmlClass c) {
        String pk = inferPrimaryKey(c);
        if (pk != null) {
            return Optional.of(pk);
        }
        if (!c.getAttributes().isEmpty()) {
            return Optional.of(c.getAttributes().get(0).getName());
        }
        return Optional.empty();
    }

    private static String sqlColumnDefinition(UmlClass owner, String attributeName) {
        for (UmlAttribute attribute : owner.getAttributes()) {
            if (attribute.getName().equals(attributeName)) {
                return sqlIdentifier(attributeName) + " " + mapUmlTypeToSql(attribute.getType());
            }
        }
        return sqlIdentifier(attributeName) + " INT";
    }

    private static List<String> variableNamesForClassAttributes(UmlClass c) {
        List<String> vars = new ArrayList<>();
        if (c.getAttributes().isEmpty()) return vars;
        char base = Character.toUpperCase(c.getClassName().charAt(0));
        for (int i = 0; i < c.getAttributes().size(); i++) vars.add(base + Integer.toString(i));
        return vars;
    }

    private static String idVar(UmlClass c) {
        List<String> vars = variableNamesForClassAttributes(c);
        if (vars.isEmpty()) return Character.toUpperCase(c.getClassName().charAt(0)) + "0";
        return vars.get(0);
    }

    private static String inferPrimaryKey(UmlClass c) {
        String lowerClassName = c.getClassName().toLowerCase();
        String candidate1 = "id";
        String candidate2 = lowerClassName + "id";
        for (UmlAttribute a : c.getAttributes()) {
            String attrLower = a.getName().toLowerCase();
            if (attrLower.equals(candidate1) || attrLower.equals(candidate2)) return a.getName();
        }
        return null;
    }

    private static String mapUmlTypeToSql(String umlType) {
        if (umlType == null) return "VARCHAR(255)";
        String lower = umlType.trim().toLowerCase();
        if (lower.equals("string")) return "VARCHAR(255)";
        if (lower.equals("integer") || lower.equals("int") || lower.equals("long")) return "INT";
        if (lower.equals("boolean") || lower.equals("bool")) return "INT";
        if (lower.equals("date") || lower.equals("localdate")) return "INT";
        return "VARCHAR(255)";
    }

    private static String sqlIdentifier(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String trimmed = name.trim();
        if (SQL_KEYWORDS.contains(trimmed.toLowerCase())) return trimmed + "_";
        return trimmed;
    }

    private static String idColumnName(UmlClass c, String fallback) {
        if (c == null || c.getAttributes().isEmpty()) return fallback;
        return c.getAttributes().get(0).getName();
    }

    private static Map<String, List<String>> associationFieldsByClass(UmlModel model) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Association a : model.getAssociations()) {
            List<AssociationEnd> ends = a.getEnds();
            if (ends.size() != 2) continue;
            AssociationEnd e0 = ends.get(0);
            AssociationEnd e1 = ends.get(1);
            if (!e0.isNavigable() && !e1.isNavigable()) continue;

            if (e1.isNavigable()) {
                boolean multiple = e1.getMax() == -1 || e1.getMax() > 1;
                String fieldName = toJsonFieldName(e1.getRoleName(), multiple);
                String typeName = multiple ? "List<" + e1.getType().getClassName() + ">" : e1.getType().getClassName();
                out.computeIfAbsent(e0.getType().getClassName(), ignored -> new ArrayList<>()).add(fieldName + ":" + typeName);
            }
            if (e0.isNavigable()) {
                boolean multiple = e0.getMax() == -1 || e0.getMax() > 1;
                String fieldName = toJsonFieldName(e0.getRoleName(), multiple);
                String typeName = multiple ? "List<" + e0.getType().getClassName() + ">" : e0.getType().getClassName();
                out.computeIfAbsent(e1.getType().getClassName(), ignored -> new ArrayList<>()).add(fieldName + ":" + typeName);
            }
        }
        return out;
    }

    private static String toJsonFieldName(String roleName, boolean multiple) {
        if (roleName == null || roleName.isBlank()) return "items";
        String s = roleName.trim();
        if (multiple && !s.endsWith("s") && !s.endsWith("List")) s = s + "s";
        return Character.toLowerCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
    }
}
