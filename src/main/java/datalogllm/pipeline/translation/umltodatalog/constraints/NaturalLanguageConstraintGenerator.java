package datalogllm.pipeline.translation.umltodatalog.constraints;

/**
 * Strategy interface for turning natural-language business rules into Datalog fragments
 * to merge with the structural schema. Implementations may call an LLM (Gemini) or
 * return a fixed / empty result (no-op, test stubs).
 */
public interface NaturalLanguageConstraintGenerator {

    /**
     * Generates Datalog constraints from natural-language constraints,
     * using structural Datalog as context.
     */
    String generateConstraints(String structuralDatalog, String naturalLanguageConstraints);
}
