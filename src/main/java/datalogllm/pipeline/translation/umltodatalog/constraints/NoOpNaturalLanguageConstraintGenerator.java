package datalogllm.pipeline.translation.umltodatalog.constraints;

/**
 * Strategy that skips NL enrichment: returns an empty Datalog fragment so merge keeps structural output only.
 */
public final class NoOpNaturalLanguageConstraintGenerator implements NaturalLanguageConstraintGenerator {

    public static final NoOpNaturalLanguageConstraintGenerator INSTANCE =
            new NoOpNaturalLanguageConstraintGenerator();

    private NoOpNaturalLanguageConstraintGenerator() {
    }

    @Override
    public String generateConstraints(String structuralDatalog, String naturalLanguageConstraints) {
        return "";
    }
}
