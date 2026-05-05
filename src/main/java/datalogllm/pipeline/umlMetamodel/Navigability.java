package datalogllm.pipeline.umlMetamodel;

import java.util.Objects;

/**
 * Metamodel representation of navigability at an association end.
 * <p>
 * Conceptually corresponds to the Datalog meta-predicate:
 * {@code navigability(AssocID, End, Bool)}.
 */
public final class Navigability {

    private final Association association;
    private final String end; // typically "source" or "target", or a role name
    private final boolean navigable;

    public Navigability(Association association, String end, boolean navigable) {
        this.association = Objects.requireNonNull(association, "association must not be null");
        if (end == null || end.isBlank()) {
            throw new IllegalArgumentException("end must not be null or blank");
        }
        this.end = end;
        this.navigable = navigable;
    }

    public Association getAssociation() {
        return association;
    }

    public String getEnd() {
        return end;
    }

    public boolean isNavigable() {
        return navigable;
    }
}



