package datalogllm.pipeline.umlMetamodel;

import java.util.Objects;

/**
 * Metamodel representation of a UML Generalization (inheritance relationship).
 * <p>
 * Conceptually corresponds to the Datalog meta-predicate:
 * {@code extends(Sub, Super)}.
 */
public final class Generalization {

    private final UmlClass subClass;
    private final UmlClass superClass;

    public Generalization(UmlClass subClass, UmlClass superClass) {
        this.subClass = Objects.requireNonNull(subClass, "subClass must not be null");
        this.superClass = Objects.requireNonNull(superClass, "superClass must not be null");
    }

    public UmlClass getSubClass() {
        return subClass;
    }

    public UmlClass getSuperClass() {
        return superClass;
    }
}



