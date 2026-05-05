package datalogllm.pipeline.umlMetamodel.services.sql;

import datalogllm.pipeline.umlMetamodel.services.sql.assertions.SqlAssertionsFromDatalog;

import java.nio.file.Path;
import java.util.Objects;

final class SqlAssertionsGenerationService {

    void generate(Path outputDirectory, Path sqlFile) {
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(sqlFile, "sqlFile must not be null");
        SqlAssertionsFromDatalog.generate(
            outputDirectory.resolve("schema.dl"),
            sqlFile,
            outputDirectory.resolve("schema-assertions.sql")
        );
    }
}
