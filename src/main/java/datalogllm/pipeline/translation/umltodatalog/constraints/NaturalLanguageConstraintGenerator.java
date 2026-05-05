package datalogllm.pipeline.translation.umltodatalog.constraints;

public interface NaturalLanguageConstraintGenerator {

    /**
     * Generates Datalog constraints from natural-language constraints,
     * using structural Datalog as context.
     */
    String generateConstraints(String structuralDatalog, String naturalLanguageConstraints);
}
