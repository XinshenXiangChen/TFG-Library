package datalogllm.pipeline.translation.umltodatalog.mapping;

import datalogllm.pipeline.umlMetamodel.Association;
import datalogllm.pipeline.umlMetamodel.AssociationEnd;
import datalogllm.pipeline.umlMetamodel.UmlClass;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.LogicSchema;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.creation.LogicSchemaBuilder;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.creation.spec.LogicConstraintWithIDSpec;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.creation.spec.helpers.DerivationRuleSpecBuilder;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.creation.spec.helpers.LogicConstraintWithIDSpecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds an IMP-Logics {@link LogicSchema} directly from {@link UmlModel},
 * mirroring the constraints and derivation rules produced by {@link UmlToLogicSchemaMapper#toDatalog(UmlModel, List)}.
 */
public final class UmlModelToLogicSchema {

    private UmlModelToLogicSchema() {
    }

    public static LogicSchema fromUmlModel(UmlModel umlModel) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        List<UmlToLogicSchemaMapper.Inheritance> inheritances = umlModel.getGeneralizations().stream()
                .map(g -> new UmlToLogicSchemaMapper.Inheritance(g.getSubClass(), g.getSuperClass()))
                .toList();
        return fromUmlModel(umlModel, inheritances);
    }

    public static LogicSchema fromUmlModel(UmlModel umlModel,
                                           List<UmlToLogicSchemaMapper.Inheritance> inheritances) {
        Objects.requireNonNull(umlModel, "umlModel must not be null");
        Objects.requireNonNull(inheritances, "inheritances must not be null");

        LogicSchemaBuilder<LogicConstraintWithIDSpec> builder =
                LogicSchemaBuilder.defaultLogicSchemaWithIDsBuilder();
        int[] nextConstraintId = {1};
        Set<String> auxPredicatesDefined = new LinkedHashSet<>();

        if (!inheritances.isEmpty()) {
            Set<UmlClass> superClasses = new LinkedHashSet<>();
            for (UmlToLogicSchemaMapper.Inheritance inh : inheritances) {
                superClasses.add(inh.superClass());
            }
            for (UmlClass superClass : superClasses) {
                addAuxPredicateIfNeeded(builder, superClass, auxPredicatesDefined);
            }
            for (UmlToLogicSchemaMapper.Inheritance inh : inheritances) {
                UmlClass subClass = inh.subClass();
                UmlClass superClass = inh.superClass();
                List<String> subVars = variableNamesForClassAttributes(subClass);
                if (subVars.isEmpty()) continue;
                String subId = idVar(subClass);
                String superAuxName = "is" + superClass.getClassName() + "Aux";
                builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                        .addConstraintId(Integer.toString(nextConstraintId[0]++))
                        .addOrdinaryLiteral(subClass.getClassName(), subVars.toArray(String[]::new))
                        .addNegatedOrdinaryLiteral(superAuxName, subId)
                        .build());
            }
        }

        Set<UmlClass> classesUsedInAssociations = new LinkedHashSet<>();
        for (Association a : umlModel.getAssociations()) {
            for (AssociationEnd end : a.getEnds()) {
                classesUsedInAssociations.add(end.getType());
            }
        }
        for (UmlClass c : classesUsedInAssociations) {
            addAuxPredicateIfNeeded(builder, c, auxPredicatesDefined);
        }

        for (Association a : umlModel.getAssociations()) {
            List<AssociationEnd> ends = a.getEnds();
            if (ends.size() != 2) continue;
            String predName = a.getName();
            UmlClass leftClass = ends.get(0).getType();
            UmlClass rightClass = ends.get(1).getType();
            String leftId = idVar(leftClass);
            String rightId = idVar(rightClass);

            builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                    .addConstraintId(Integer.toString(nextConstraintId[0]++))
                    .addOrdinaryLiteral(predName, leftId, rightId)
                    .addNegatedOrdinaryLiteral("is" + leftClass.getClassName() + "Aux", leftId)
                    .build());

            builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                    .addConstraintId(Integer.toString(nextConstraintId[0]++))
                    .addOrdinaryLiteral(predName, leftId, rightId)
                    .addNegatedOrdinaryLiteral("is" + rightClass.getClassName() + "Aux", rightId)
                    .build());
        }

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
                builder.addDerivationRule(new DerivationRuleSpecBuilder()
                        .addHead(minPredName, rightId)
                        .addOrdinaryLiteral(predName, leftId, rightId)
                        .build());
                List<String> classVars = variableNamesForClassAttributes(rightClass);
                if (!classVars.isEmpty()) {
                    builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                            .addConstraintId(Integer.toString(nextConstraintId[0]++))
                            .addOrdinaryLiteral(rightClass.getClassName(), classVars.toArray(String[]::new))
                            .addNegatedOrdinaryLiteral(minPredName, rightId)
                            .build());
                }
            }

            if (rightEnd.getMin() >= 1) {
                String minPredName = "Min" + predName + "_" + rightClass.getClassName();
                builder.addDerivationRule(new DerivationRuleSpecBuilder()
                        .addHead(minPredName, leftId)
                        .addOrdinaryLiteral(predName, leftId, rightId)
                        .build());
                List<String> classVars = variableNamesForClassAttributes(leftClass);
                if (!classVars.isEmpty()) {
                    builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                            .addConstraintId(Integer.toString(nextConstraintId[0]++))
                            .addOrdinaryLiteral(leftClass.getClassName(), classVars.toArray(String[]::new))
                            .addNegatedOrdinaryLiteral(minPredName, leftId)
                            .build());
                }
            }

            if (leftEnd.getMax() == 1) {
                builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                        .addConstraintId(Integer.toString(nextConstraintId[0]++))
                        .addOrdinaryLiteral(predName, "L0", rightId)
                        .addOrdinaryLiteral(predName, "L1", rightId)
                        .addBuiltInLiteral("<>", "L1", "L0")
                        .build());
            }
            if (rightEnd.getMax() == 1) {
                builder.addLogicConstraint(new LogicConstraintWithIDSpecBuilder()
                        .addConstraintId(Integer.toString(nextConstraintId[0]++))
                        .addOrdinaryLiteral(predName, leftId, "R0")
                        .addOrdinaryLiteral(predName, leftId, "R1")
                        .addBuiltInLiteral("<>", "R1", "R0")
                        .build());
            }
        }

        return builder.build();
    }

    private static void addAuxPredicateIfNeeded(LogicSchemaBuilder<LogicConstraintWithIDSpec> builder,
                                                UmlClass c,
                                                Set<String> auxPredicatesDefined) {
        List<String> vars = variableNamesForClassAttributes(c);
        if (vars.isEmpty()) return;
        String auxName = "is" + c.getClassName() + "Aux";
        if (!auxPredicatesDefined.add(auxName)) return;
        builder.addDerivationRule(new DerivationRuleSpecBuilder()
                .addHead(auxName, vars.get(0))
                .addOrdinaryLiteral(c.getClassName(), vars.toArray(String[]::new))
                .build());
    }

    private static List<String> variableNamesForClassAttributes(UmlClass c) {
        List<String> vars = new ArrayList<>();
        if (c.getAttributes().isEmpty()) return vars;
        char base = Character.toUpperCase(c.getClassName().charAt(0));
        for (int i = 0; i < c.getAttributes().size(); i++) {
            vars.add(base + Integer.toString(i));
        }
        return vars;
    }

    private static String idVar(UmlClass c) {
        List<String> vars = variableNamesForClassAttributes(c);
        if (vars.isEmpty()) return Character.toUpperCase(c.getClassName().charAt(0)) + "0";
        return vars.get(0);
    }
}
