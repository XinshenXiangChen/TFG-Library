package datalogllm.pipeline.translation.umltodatalog.mapping;

import datalogllm.pipeline.umlMetamodel.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class UmlToLogicSchemaMapper {
    private static final Set<String> SQL_KEYWORDS = Set.of(
        "date", "time", "timestamp", "year", "month", "day",
        "select", "from", "where", "group", "order", "by",
        "table", "create", "drop", "insert", "update", "delete",
        "primary", "key", "constraint", "assertion", "join"
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

        StringBuilder sb = new StringBuilder();
        sb.append("%+ Classes and associations\n\n");

        for (UmlClass c : umlModel.getClasses()) {
            List<String> attrVars = variableNamesForClassAttributes(c);
            sb.append("%  ").append(c.getClassName()).append("(")
                .append(String.join(", ", attrVars))
                .append(")\n");
        }

        for (Association a : umlModel.getAssociations()) {
            List<AssociationEnd> ends = a.getEnds();
            if (ends.size() != 2) continue;
            UmlClass leftClass = ends.get(0).getType();
            UmlClass rightClass = ends.get(1).getType();
            String leftVar = idVar(leftClass);
            String rightVar = idVar(rightClass);
            String predName = a.getName();
            sb.append("%  ").append(predName)
                .append("(").append(leftVar).append(", ").append(rightVar).append(")\n");
        }
        sb.append("\n");

        int[] nextConstraintId = {1};
        Set<String> auxPredicatesDefined = new LinkedHashSet<>();

        if (!inheritances.isEmpty()) {
            sb.append("%+ Constraints for hierarchies\n\n");
            Set<UmlClass> superClasses = new LinkedHashSet<>();
            for (Inheritance inh : inheritances) superClasses.add(inh.superClass());
            for (UmlClass superClass : superClasses) appendAuxPredicateIfNeeded(sb, superClass, auxPredicatesDefined);

            for (Inheritance inh : inheritances) {
                UmlClass subClass = inh.subClass();
                UmlClass superClass = inh.superClass();
                List<String> subVars = variableNamesForClassAttributes(subClass);
                if (subVars.isEmpty()) continue;
                String subId = idVar(subClass);
                String superAuxName = "is" + superClass.getClassName() + "Aux";
                sb.append("  @").append(nextConstraintId[0]++)
                    .append(" :- ")
                    .append(subClass.getClassName())
                    .append("(").append(String.join(", ", subVars)).append("), ")
                    .append("not(").append(superAuxName).append("(").append(subId).append("))\n");
            }
            sb.append("\n");
        }

        sb.append("%+ Constraints for associations\n\n");
        Set<UmlClass> classesUsedInAssociations = new LinkedHashSet<>();
        for (Association a : umlModel.getAssociations()) {
            for (AssociationEnd end : a.getEnds()) classesUsedInAssociations.add(end.getType());
        }
        for (UmlClass c : classesUsedInAssociations) appendAuxPredicateIfNeeded(sb, c, auxPredicatesDefined);
        sb.append("\n");

        for (Association a : umlModel.getAssociations()) {
            List<AssociationEnd> ends = a.getEnds();
            if (ends.size() != 2) continue;
            String predName = a.getName();
            UmlClass leftClass = ends.get(0).getType();
            UmlClass rightClass = ends.get(1).getType();
            String leftId = idVar(leftClass);
            String rightId = idVar(rightClass);

            sb.append("  @").append(nextConstraintId[0]++)
                .append(" :- ").append(predName)
                .append("(").append(leftId).append(", ").append(rightId).append("), ")
                .append("not(is").append(leftClass.getClassName()).append("Aux(").append(leftId).append("))\n");

            sb.append("  @").append(nextConstraintId[0]++)
                .append(" :- ").append(predName)
                .append("(").append(leftId).append(", ").append(rightId).append("), ")
                .append("not(is").append(rightClass.getClassName()).append("Aux(").append(rightId).append("))\n");
        }
        sb.append("\n");

        sb.append("%+ Constraints for association cardinalities\n\n");
        for (Association a : umlModel.getAssociations()) {
            List<AssociationEnd> ends = a.getEnds();
            if (ends.size() != 2) continue;
            String predName = a.getName();
            AssociationEnd leftEnd = ends.get(0);
            AssociationEnd rightEnd = ends.get(1);
            UmlClass leftClass = leftEnd.getType();
            UmlClass rightClass = rightEnd.getType();
            String leftId = idVar(leftClass);
            String rightId = idVar(rightClass);

            if (leftEnd.getMin() >= 1) {
                String minPredName = "Min" + predName + "_" + leftClass.getClassName();
                sb.append("  ").append(minPredName).append("(").append(rightId).append(") :- ")
                    .append(predName).append("(").append(leftId).append(", ").append(rightId).append(")\n");
                List<String> classVars = variableNamesForClassAttributes(rightClass);
                if (!classVars.isEmpty()) {
                    sb.append("  @").append(nextConstraintId[0]++)
                        .append(" :- ").append(rightClass.getClassName())
                        .append("(").append(String.join(", ", classVars)).append("), ")
                        .append("not(").append(minPredName).append("(").append(rightId).append("))\n");
                }
            }

            if (rightEnd.getMin() >= 1) {
                String minPredName = "Min" + predName + "_" + rightClass.getClassName();
                sb.append("  ").append(minPredName).append("(").append(leftId).append(") :- ")
                    .append(predName).append("(").append(leftId).append(", ").append(rightId).append(")\n");
                List<String> classVars = variableNamesForClassAttributes(leftClass);
                if (!classVars.isEmpty()) {
                    sb.append("  @").append(nextConstraintId[0]++)
                        .append(" :- ").append(leftClass.getClassName())
                        .append("(").append(String.join(", ", classVars)).append("), ")
                        .append("not(").append(minPredName).append("(").append(leftId).append("))\n");
                }
            }

            if (leftEnd.getMax() == 1) {
                sb.append("  @").append(nextConstraintId[0]++)
                    .append(" :- ").append(predName).append("(L0, ").append(rightId).append("), ")
                    .append(predName).append("(L1, ").append(rightId).append("), L1<>L0\n");
            }
            if (rightEnd.getMax() == 1) {
                sb.append("  @").append(nextConstraintId[0]++)
                    .append(" :- ").append(predName).append("(").append(leftId).append(", R0), ")
                    .append(predName).append("(").append(leftId).append(", R1), R1<>R0\n");
            }
        }
        return sb.toString();
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
            String leftColumn = idColumnName(ends.get(0).getType(), "leftId");
            String rightColumn = idColumnName(ends.get(1).getType(), "rightId");
            sb.append("CREATE TABLE ")
                .append(sqlIdentifier(association.getName()))
                .append(" (\n")
                .append("    ").append(sqlIdentifier(leftColumn)).append(" INT,\n")
                .append("    ").append(sqlIdentifier(rightColumn)).append(" INT\n")
                .append(");\n\n");
        }
        return sb.toString();
    }

    private static void appendAuxPredicateIfNeeded(StringBuilder sb, UmlClass c, Set<String> auxPredicatesDefined) {
        List<String> vars = variableNamesForClassAttributes(c);
        if (vars.isEmpty()) return;
        String auxName = "is" + c.getClassName() + "Aux";
        if (!auxPredicatesDefined.add(auxName)) return;
        sb.append(auxName)
            .append("(").append(vars.get(0)).append(") :- ")
            .append(c.getClassName())
            .append("(").append(String.join(", ", vars)).append(")\n");
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
