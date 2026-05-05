package datalogllm.pipeline.umlMetamodel.services.sql.assertions;

import edu.upc.fib.inlab.imp.kse.blast.BLASTranslator;
import edu.upc.fib.inlab.imp.kse.blast.translations.DatalogToSQLTranslationInfo;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.LogicSchema;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.parser.LogicSchemaWithIDsParser;
import edu.upc.fib.inlab.imp.kse.sql.core.schema.Assertion;
import edu.upc.fib.inlab.imp.kse.sql.core.schema.SQLObjectSchema;
import edu.upc.fib.inlab.imp.kse.sql.core.services.parser.StandardSQLParser;
import edu.upc.fib.inlab.imp.kse.sql.sql_server.services.printer.SQLServerPrinter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class SqlAssertionsFromDatalog {
    private SqlAssertionsFromDatalog() {}

    public static void generate(Path datalogSchemaFile, Path sqlSchemaFile, Path outputAssertionsFile) {
        Objects.requireNonNull(datalogSchemaFile, "datalogSchemaFile must not be null");
        Objects.requireNonNull(sqlSchemaFile, "sqlSchemaFile must not be null");
        Objects.requireNonNull(outputAssertionsFile, "outputAssertionsFile must not be null");
        LogicSchema logicSchema = parseLogicSchema(datalogSchemaFile);
        SQLObjectSchema sqlSchema = parseSqlSchema(sqlSchemaFile);
        BLASTranslator translator = BLASTranslator.one2one(sqlSchema);
        DatalogToSQLTranslationInfo translationInfo = translator.translateDatalogToSql(List.of(), logicSchema, List.of());
        writeAssertions(outputAssertionsFile, translationInfo.assertions());
    }

    private static LogicSchema parseLogicSchema(Path datalogSchemaFile) {
        try {
            String datalog = Files.readString(datalogSchemaFile, StandardCharsets.UTF_8);
            return new LogicSchemaWithIDsParser().parse(datalog);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Datalog schema from " + datalogSchemaFile, e);
        }
    }

    private static SQLObjectSchema parseSqlSchema(Path sqlSchemaFile) {
        try {
            String sql = Files.readString(sqlSchemaFile, StandardCharsets.UTF_8);
            String normalizedSql = sql.replace("\"", "").replaceAll("(?i)\\bBOOLEAN\\b", "INT");
            StandardSQLParser parser = new StandardSQLParser();
            parser.parse(normalizedSql);
            return parser.getSQLObjectSchema();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read SQL schema from " + sqlSchemaFile, e);
        }
    }

    private static void writeAssertions(Path outputFile, List<Assertion> assertions) {
        SQLServerPrinter printer = new SQLServerPrinter();
        StringBuilder sb = new StringBuilder();
        for (Assertion assertion : assertions) sb.append(assertion.visit(printer)).append(";\n\n");
        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write SQL assertions to " + outputFile, e);
        }
    }
}
