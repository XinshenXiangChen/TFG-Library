package datalogllm.pipeline.umlMetamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Root container for the UML metamodel instances extracted from PlantUML.
 * <p>
 * This is the entry point that can later be translated into the IMP Logics
 * Datalog metamodel (LogicSchema) and then printed as concrete Datalog.
 */
public final class UmlModel {

    private final List<UmlClass> classes = new ArrayList<>();
    private final List<Association> associations = new ArrayList<>();
    private final List<AssociationEnd> associationEnds = new ArrayList<>();
    private final List<Generalization> generalizations = new ArrayList<>();
    private String naturalLanguageConstraints = "";

    public List<UmlClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<Association> getAssociations() {
        return Collections.unmodifiableList(associations);
    }

    public List<AssociationEnd> getAssociationEnds() {
        return Collections.unmodifiableList(associationEnds);
    }

    public List<Generalization> getGeneralizations() {
        return Collections.unmodifiableList(generalizations);
    }

    public String getNaturalLanguageConstraints() {
        return naturalLanguageConstraints;
    }

    public void addClass(UmlClass umlClass) {
        classes.add(Objects.requireNonNull(umlClass, "umlClass must not be null"));
    }

    public void addAssociation(Association association) {
        associations.add(Objects.requireNonNull(association, "association must not be null"));
    }

    public void addAssociationEnd(AssociationEnd end) {
        associationEnds.add(Objects.requireNonNull(end, "associationEnd must not be null"));
    }

    public void addGeneralization(Generalization generalization) {
        generalizations.add(Objects.requireNonNull(generalization, "generalization must not be null"));
    }

    public void setNaturalLanguageConstraints(String naturalLanguageConstraints) {
        this.naturalLanguageConstraints = Objects.requireNonNullElse(naturalLanguageConstraints, "").trim();
    }
}



