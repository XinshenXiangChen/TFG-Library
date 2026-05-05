package datalogllm.pipeline.translation.umltodatalog.utils;

import datalogllm.pipeline.translation.umltodatalog.constraints.GeminiNaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UmlToDatalogUtils {
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private UmlToDatalogUtils() {
    }

    public static void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + path, e);
        }
    }

    public static NaturalLanguageConstraintGenerator fromEnvironmentGeminiGenerator() {
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
        return new GeminiNaturalLanguageConstraintGenerator(apiKey, DEFAULT_GEMINI_MODEL);
    }

    public static NaturalLanguageConstraintGenerator fromApiKey(String geminiApiKey) {
        return new GeminiNaturalLanguageConstraintGenerator(geminiApiKey, DEFAULT_GEMINI_MODEL);
    }
}
