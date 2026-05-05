package datalogllm.pipeline.umlMetamodel;

import java.util.Objects;

/**
 * Metamodel representation of a UML Attribute.
 * <p>
 * Conceptually corresponds to the Datalog meta-predicate:
 * {@code has_attribute(Class, Name, Type)}.
 */
public final class UmlAttribute {

    private final UmlClass ownerClass;
    private final String name;
    private final String type;

    public UmlAttribute(UmlClass ownerClass, String name, String type) {
        this.ownerClass = Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be null or blank");
        }
        this.name = name;
        this.type = type;
    }

    public UmlClass getOwnerClass() {
        return ownerClass;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}



