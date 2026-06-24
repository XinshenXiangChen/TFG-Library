package datalogllm.pipeline.translation.umltodatalog.utils;

import datalogllm.pipeline.translation.umltodatalog.constraints.ConstraintGeneratorFactory;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.LogicSchema;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.printer.LogicSchemaPrinter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class UmlToDatalogUtils {

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

    /**
     * Serializes a {@link LogicSchema} using IMP-Logics {@link LogicSchemaPrinter}
     * (same formatting as the domain {@code toString()} on constraints and rules).
     */
    public static String printLogicSchema(LogicSchema logicSchema) {
        Objects.requireNonNull(logicSchema, "logicSchema must not be null");
        return new LogicSchemaPrinter().print(logicSchema);
    }

    public static void writeLogicSchema(Path path, LogicSchema logicSchema) {
        write(path, printLogicSchema(logicSchema));
    }

    public static NaturalLanguageConstraintGenerator fromEnvironmentGeminiGenerator() {
        return ConstraintGeneratorFactory.fromEnvironment();
    }

    public static NaturalLanguageConstraintGenerator fromApiKey(String geminiApiKey) {
        return ConstraintGeneratorFactory.fromApiKey(geminiApiKey);
    }
}
