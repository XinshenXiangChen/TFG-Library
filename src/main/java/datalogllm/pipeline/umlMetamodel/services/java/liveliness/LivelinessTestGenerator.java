package datalogllm.pipeline.umlMetamodel.services.java.liveliness;

import datalogllm.pipeline.umlMetamodel.UmlClass;
import datalogllm.pipeline.umlMetamodel.UmlModel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class LivelinessTestGenerator {
    private final String testPackage;
    private final Path outputDirectory;

    public LivelinessTestGenerator(String testPackage, Path outputDirectory) {
        this.testPackage = testPackage == null ? "" : testPackage.trim();
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }

    public void generate(UmlModel model, Path schemaFilePath) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(schemaFilePath, "schemaFilePath must not be null");
        Path base = outputDirectory;
        if (!testPackage.isEmpty()) base = outputDirectory.resolve(testPackage.replace('.', '/'));
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create output directory: " + base, e);
        }
        String pathForGeneratedCode = schemaFilePath.toString().replace('\\', '/');
        for (UmlClass c : model.getClasses()) {
            if (!isValidTestClassName(c.getClassName())) continue;
            String testSource = buildTestClass(c, pathForGeneratedCode);
            Path file = base.resolve(c.getClassName() + "LivelinessTest.java");
            try {
                Files.writeString(file, testSource, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write " + file, e);
            }
        }
    }

    private static boolean isValidTestClassName(String name) {
        return name != null && name.length() >= 2 && Character.isUpperCase(name.charAt(0));
    }

    private String buildTestClass(UmlClass c, String schemaPathForCode) {
        String className = c.getClassName();
        String pathConstant = schemaPathForCode.replace("\\", "\\\\").replace("\"", "\\\"");
        StringBuilder sb = new StringBuilder();
        if (!testPackage.isEmpty()) sb.append("package ").append(testPackage).append(";\n\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.*;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.parser.LogicSchemaWithIDsParser;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.reasoner.Goal;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.reasoner.Reasoner;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.reasoner.ReasonerProperties;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.reasoner.ReasonerResult;\n");
        sb.append("import edu.upc.fib.inlab.imp.kse.reasoner.SATResult;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("import java.nio.charset.StandardCharsets;\n");
        sb.append("import java.nio.file.Files;\n");
        sb.append("import java.nio.file.Path;\n");
        sb.append("import java.util.List;\n\n");
        sb.append("import static org.assertj.core.api.Assertions.assertThat;\n\n");
        sb.append("class ").append(className).append("LivelinessTest {\n\n");
        sb.append("    private static final String SCHEMA_PATH = \"").append(pathConstant).append("\";\n\n");
        sb.append("    @Test\n");
        sb.append("    void ").append(className.toLowerCase()).append("IsSatisfiable() throws java.io.IOException {\n");
        sb.append("        String schema = Files.readString(Path.of(SCHEMA_PATH), StandardCharsets.UTF_8);\n");
        sb.append("        LogicSchema logicSchema = new LogicSchemaWithIDsParser().parse(schema);\n");
        sb.append("        Predicate predicate = logicSchema.getPredicateByName(\"").append(className).append("\");\n");
        sb.append("        List<Term> terms = new java.util.ArrayList<>();\n");
        sb.append("        for (int i = 0; i < predicate.getArity(); i++) terms.add(new Variable(\"Var\" + i));\n");
        sb.append("        Goal goal = new Goal(new OrdinaryLiteral(new Atom(predicate, terms)));\n");
        sb.append("        Reasoner reasoner = new Reasoner(logicSchema);\n");
        sb.append("        ReasonerProperties properties = ReasonerProperties.builder().notifyEvents(false).build();\n");
        sb.append("        ReasonerResult result = reasoner.isSatisfiable(goal, properties);\n");
        sb.append("        assertThat(result).isInstanceOf(SATResult.class);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
