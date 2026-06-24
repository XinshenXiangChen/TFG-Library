package datalogllm.pipeline.translation.umltodatalog.constraints;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Factory for {@link NaturalLanguageConstraintGenerator} strategy implementations.
 */
public final class ConstraintGeneratorFactory {

    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private ConstraintGeneratorFactory() {
    }

    /** No NL enrichment (structural Datalog only). */
    public static NaturalLanguageConstraintGenerator noOp() {
        return NoOpNaturalLanguageConstraintGenerator.INSTANCE;
    }

    /** Gemini using {@code GEMINI_API_KEY} from the environment or {@code .env}. */
    public static NaturalLanguageConstraintGenerator fromEnvironment() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            apiKey = dotenv.get("GEMINI_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing GEMINI_API_KEY (env or .env)");
        }
        return fromApiKey(apiKey);
    }

    public static NaturalLanguageConstraintGenerator fromApiKey(String geminiApiKey) {
        return new GeminiNaturalLanguageConstraintGenerator(geminiApiKey, DEFAULT_GEMINI_MODEL);
    }
}
