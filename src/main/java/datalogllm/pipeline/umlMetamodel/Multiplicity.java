package datalogllm.pipeline.umlMetamodel;

import java.util.Objects;

/**
 * Metamodel representation of multiplicity information at an association end.
 * <p>
 * Conceptually corresponds to the Datalog meta-predicate:
 * {@code multiplicity(AssocID, End, Min, Max)}.
 */
public final class Multiplicity {

    private final Association association;
    private final String end; // typically "source" or "target", or a role name
    private final int min;
    private final int max; // use -1 to represent * (unbounded)

    public Multiplicity(Association association, String end, int min, int max) {
        this.association = Objects.requireNonNull(association, "association must not be null");
        if (end == null || end.isBlank()) {
            throw new IllegalArgumentException("end must not be null or blank");
        }
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max != -1 && max < min) {
            throw new IllegalArgumentException("max must be -1 (for *) or >= min");
        }
        this.end = end;
        this.min = min;
        this.max = max;
    }

    public Association getAssociation() {
        return association;
    }

    public String getEnd() {
        return end;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}



