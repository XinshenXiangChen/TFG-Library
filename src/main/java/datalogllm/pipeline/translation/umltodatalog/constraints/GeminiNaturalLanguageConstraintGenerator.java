package datalogllm.pipeline.translation.umltodatalog.constraints;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import java.util.Objects;

public final class GeminiNaturalLanguageConstraintGenerator implements NaturalLanguageConstraintGenerator {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final Client client;
    private final String model;

    public GeminiNaturalLanguageConstraintGenerator(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public GeminiNaturalLanguageConstraintGenerator(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be null or blank");
        }
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
    }

    @Override
    public String generateConstraints(String structuralDatalog, String naturalLanguageConstraints) {
        Objects.requireNonNull(structuralDatalog, "structuralDatalog must not be null");
        Objects.requireNonNull(naturalLanguageConstraints, "naturalLanguageConstraints must not be null");
        if (naturalLanguageConstraints.isBlank()) {
            return "";
        }

        String prompt = """
            You are given:
            1) Structural Datalog generated from UML classes/associations.
            2) Natural-language constraints.

            Generate ONLY additional Datalog constraints for the natural-language part.

            Rules:
            - Reuse existing predicates and arities from structural Datalog.
            - Do not redefine class/association schema comments.
            - Output plain text only (no markdown, no explanations).
            - Constraints must use format '@N :- ...'
            - If helpful, you may define auxiliary predicates.
            - Use safe, deterministic names for auxiliary predicates.

            Structural Datalog:
            ---
            %s
            ---

            Natural-language constraints:
            ---
            %s
            ---
            """.formatted(structuralDatalog, naturalLanguageConstraints);

        System.out.println("=== Gemini NL Constraint Request ===");
        System.out.println(prompt);
        System.out.println("=== End Gemini Request ===");

        GenerateContentConfig config = GenerateContentConfig.builder()
            .temperature(0.1F)
            .build();

        GenerateContentResponse response = client.models.generateContent(model, prompt, config);
        String text = response.text();
        String output = text == null ? "" : text.trim();

        System.out.println("=== Gemini NL Constraint Response ===");
        System.out.println(output);
        System.out.println("=== End Gemini Response ===");

        return output;
    }
}
