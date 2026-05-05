package datalogllm.uml.translation.umlmetamodel.sql.assertions;

import datalogllm.pipeline.PlantUmlPipeline;
import datalogllm.pipeline.umlMetamodel.services.sql.assertions.SqlAssertionsFromDatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlAssertionsFromDatalogIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void generate_fromFreshPipelineOutput_createsSqlAssertions() throws IOException {
        String uml = """
                @startuml
                class User {
                  + String nif
                  + String name
                }
                class Ticket {
                  + Integer id
                }
                User "1" -- "0..*" Ticket : opens
                @enduml
                """;

        Path generationDir = tempDir.resolve("generated");
        Files.createDirectories(generationDir);
        PlantUmlPipeline.generateDatalogAndJson(uml, generationDir);
        PlantUmlPipeline.generateSqlAndAssertionsFromUmlMetamodel(uml, generationDir);

        Path datalogSchemaFile = generationDir.resolve("schema.dl");
        Path sqlSchemaFile = generationDir.resolve("schema.sql");
        assertThat(datalogSchemaFile).exists();
        assertThat(sqlSchemaFile).exists();

        Path outputAssertionsFile = tempDir.resolve("schema-assertions.sql");
        SqlAssertionsFromDatalog.generate(datalogSchemaFile, sqlSchemaFile, outputAssertionsFile);
        String generatedSql = Files.readString(outputAssertionsFile, StandardCharsets.UTF_8);

        assertThat(outputAssertionsFile).exists();
        assertThat(generatedSql)
            .containsIgnoringCase("create assertion")
            .isNotBlank();
    }
}
