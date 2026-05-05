package datalogllm.pipeline.umlMetamodel;

import java.util.Objects;

/**
 * Metamodel representation of an association end, following:
 *
 * <pre>
 * class AssociationEnd {
 *     + String roleName
 *     + int min
 *     + int max
 *     + boolean isNavigable
 *     ==
 *     assoc_end(AssocID, ClassName, Role, Min, Max, IsNav)
 * }
 * </pre>
 */
public final class AssociationEnd {

    private final Association association;
    private final UmlClass type;
    private final String roleName;
    private final int min;
    private final int max; // -1 represents *
    private final boolean navigable;

    public AssociationEnd(Association association,
                          UmlClass type,
                          String roleName,
                          int min,
                          int max,
                          boolean navigable) {
        this.association = Objects.requireNonNull(association, "association must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("roleName must not be null or blank");
        }
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max != -1 && max < min) {
            throw new IllegalArgumentException("max must be -1 or >= min");
        }
        this.roleName = roleName;
        this.min = min;
        this.max = max;
        this.navigable = navigable;

        // Register back in the association
        association.addEnd(this);
    }

    public Association getAssociation() {
        return association;
    }

    public UmlClass getType() {
        return type;
    }

    public String getRoleName() {
        return roleName;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isNavigable() {
        return navigable;
    }
}



