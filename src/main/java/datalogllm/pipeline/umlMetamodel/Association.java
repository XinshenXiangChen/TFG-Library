package datalogllm.pipeline.umlMetamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metamodel representation of a UML Association.
 * <p>
 * Updated to follow the new UML metamodel:
 *
 * <pre>
 * class Association {
 *     + String id
 *     + String name
 *     ==
 *     association(ID, Name)
 * }
 *
 * class AssociationEnd {
 *     + String roleName
 *     + int min
 *     + int max
 *     + boolean isNavigable
 *     ==
 *     assoc_end(AssocID, ClassName, Role, Min, Max, IsNav)
 * }
 * </pre>
 *
 * For backward compatibility, {@code sourceClass} and {@code targetClass}
 * are still kept as a convenience view for binary associations created
 * by previous code paths.
 */
public final class Association {

    private final String id;
    private final String name;

    // Optional convenience fields for binary associations
    private final UmlClass sourceClass;
    private final UmlClass targetClass;

    private final List<AssociationEnd> ends = new ArrayList<>();

    /**
     * New constructor preferred for the metamodel: only id and name.
     */
    public Association(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        this.id = id;
        this.name = name;
        this.sourceClass = null;
        this.targetClass = null;
    }

    /**
     * Legacy constructor kept so existing parsing/mapping code that expects
     * a binary association between two classes continues to work.
     */
    public Association(String id, UmlClass sourceClass, UmlClass targetClass) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        this.id = id;
        this.name = sourceClass.getClassName() + "_" + targetClass.getClassName();
        this.sourceClass = Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass must not be null");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UmlClass getSourceClass() {
        return sourceClass;
    }

    public UmlClass getTargetClass() {
        return targetClass;
    }

    public List<AssociationEnd> getEnds() {
        return Collections.unmodifiableList(ends);
    }

    void addEnd(AssociationEnd end) {
        ends.add(Objects.requireNonNull(end, "end must not be null"));
    }
}



