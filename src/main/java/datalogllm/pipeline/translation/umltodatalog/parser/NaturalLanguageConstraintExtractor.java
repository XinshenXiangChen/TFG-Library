package datalogllm.pipeline.translation.umltodatalog.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extracts natural-language constraints from a PlantUML text.
 * <p>
 * Current convention: constraints are written after {@code @enduml}.
 */
public final class NaturalLanguageConstraintExtractor {

    private NaturalLanguageConstraintExtractor() {
    }

    public static String extract(String plantUml) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");

        String[] lines = plantUml.split("\\R");
        boolean afterEndUml = false;
        List<String> out = new ArrayList<>();

        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.equalsIgnoreCase("@enduml")) {
                afterEndUml = true;
                continue;
            }
            if (!afterEndUml || line.isBlank()) {
                continue;
            }
            out.add(line);
        }

        return String.join("\n", out).trim();
    }
}
