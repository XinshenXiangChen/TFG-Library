package datalogllm.pipeline.umlMetamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metamodel representation of a UML Class.
 * <p>
 * Conceptually corresponds to the Datalog meta-predicate:
 * {@code is_class(ClassName)}.
 */
public final class UmlClass {

    private final String className;
    private final List<UmlAttribute> attributes = new ArrayList<>();

    public UmlClass(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className must not be null or blank");
        }
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public List<UmlAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void addAttribute(UmlAttribute attribute) {
        Objects.requireNonNull(attribute, "attribute must not be null");
        attributes.add(attribute);
    }
}



