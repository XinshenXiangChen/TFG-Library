package datalogllm.pipeline.translation.umltodatalog.constraints;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatalogConstraintMerger {

    private static final Pattern CONSTRAINT_ID_PATTERN = Pattern.compile("@(\\d+)");

    private DatalogConstraintMerger() {
    }

    public static String merge(String structuralDatalog, String llmConstraints) {
        Objects.requireNonNull(structuralDatalog, "structuralDatalog must not be null");
        if (llmConstraints == null || llmConstraints.isBlank()) {
            return structuralDatalog;
        }

        int nextId = findMaxConstraintId(structuralDatalog) + 1;
        AtomicInteger idCounter = new AtomicInteger(nextId);

        Matcher matcher = CONSTRAINT_ID_PATTERN.matcher(llmConstraints);
        String renumbered = matcher.replaceAll((MatchResult mr) -> "@" + idCounter.getAndIncrement());

        return structuralDatalog.stripTrailing()
            + "\n\n%+ OCL constraints (LLM)\n"
            + renumbered.strip()
            + "\n";
    }

    private static int findMaxConstraintId(String datalog) {
        Matcher matcher = CONSTRAINT_ID_PATTERN.matcher(datalog);
        int max = 0;
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            if (id > max) {
                max = id;
            }
        }
        return max;
    }
}
