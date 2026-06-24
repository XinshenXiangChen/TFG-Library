package datalogllm.pipeline.umlMetamodel.services.sql;

import datalogllm.pipeline.PipelineConfig;
import datalogllm.pipeline.translation.umltodatalog.mapping.UmlToLogicSchemaMapper;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class UmlToSqlTranslationService {

    private final UmlParser umlParser;

    public UmlToSqlTranslationService() {
        this(UmlParser.defaultParser());
    }

    public UmlToSqlTranslationService(UmlParser umlParser) {
        this.umlParser = Objects.requireNonNull(umlParser, "umlParser must not be null");
    }

    public UmlToSqlTranslationService(PipelineConfig config) {
        this(config.umlParser());
    }

    public void translate(String plantUml, Path outputDirectory) {
        translateSqlSchema(plantUml, outputDirectory);
        translateSqlAssertions(outputDirectory);
    }

    /** Generates {@code schema.sql} from UML (no assertions). */
    public void translateSqlSchema(String plantUml, Path outputDirectory) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        String sql = new UmlToLogicSchemaMapper().toSqlSchema(umlParser.parse(plantUml));
        Path sqlFile = outputDirectory.resolve("schema.sql");
        try {
            Files.createDirectories(outputDirectory);
            Files.writeString(sqlFile, sql, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate SQL from UML metamodel", e);
        }
    }

    /**
     * Generates {@code schema-assertions.sql} via BLAST. Requires {@code schema.dl} and
     * {@code schema.sql} in {@code outputDirectory}.
     */
    public void translateSqlAssertions(Path outputDirectory) {
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Path sqlFile = outputDirectory.resolve("schema.sql");
        new SqlAssertionsGenerationService().generate(outputDirectory, sqlFile);
    }
}
