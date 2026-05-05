package datalogllm.pipeline.umlMetamodel.services.sql;

import datalogllm.pipeline.translation.umltodatalog.mapping.UmlToLogicSchemaMapper;
import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class UmlToSqlTranslationService {

    public void translate(String plantUml, Path outputDirectory) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        String sql = new UmlToLogicSchemaMapper().toSqlSchema(PlantUmlParser.parse(plantUml));
        Path sqlFile = outputDirectory.resolve("schema.sql");
        try {
            Files.createDirectories(outputDirectory);
            Files.writeString(sqlFile, sql, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate SQL from UML metamodel", e);
        }
        new SqlAssertionsGenerationService().generate(outputDirectory, sqlFile);
    }
}
