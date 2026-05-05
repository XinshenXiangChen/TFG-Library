package datalogllm.pipeline.translation.umltodatalog;

import datalogllm.pipeline.translation.umltodatalog.constraints.DatalogConstraintMerger;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.mapping.UmlToLogicSchemaMapper;
import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;
import datalogllm.pipeline.translation.umltodatalog.utils.UmlToDatalogUtils;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.parser.LogicSchemaWithIDsParser;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Translation unit: UML -> Datalog.
 */
public final class UmlToDatalog {

    private UmlToDatalog() {
    }

    public static void generate(String plantUml, Path outputDirectory) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        UmlModel model = PlantUmlParser.parse(plantUml);
        UmlToLogicSchemaMapper mapper = new UmlToLogicSchemaMapper();
        String datalog = mapper.toDatalog(model);
        UmlToDatalogUtils.write(outputDirectory.resolve("schema.dl"), datalog);
    }

    public static void generateWithLlmConstraints(String plantUml,
                                                  Path outputDirectory,
                                                  NaturalLanguageConstraintGenerator constraintGenerator) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(constraintGenerator, "constraintGenerator must not be null");

        UmlModel model = PlantUmlParser.parse(plantUml);
        UmlToLogicSchemaMapper mapper = new UmlToLogicSchemaMapper();
        String structuralDatalog = mapper.toDatalog(model);
        String nlConstraints = model.getNaturalLanguageConstraints();
        String llmDatalog = constraintGenerator.generateConstraints(structuralDatalog, nlConstraints);
        String mergedDatalog = DatalogConstraintMerger.merge(structuralDatalog, llmDatalog);
        validateDatalog(mergedDatalog);

        UmlToDatalogUtils.write(outputDirectory.resolve("schema.dl"), mergedDatalog);
    }

    public static void generateWithLlmConstraints(String plantUml, Path outputDirectory) {
        generateWithLlmConstraints(plantUml, outputDirectory, UmlToDatalogUtils.fromEnvironmentGeminiGenerator());
    }

    public static void generateWithLlmConstraints(String plantUml, Path outputDirectory, String geminiApiKey) {
        generateWithLlmConstraints(plantUml, outputDirectory, UmlToDatalogUtils.fromApiKey(geminiApiKey));
    }

    private static void validateDatalog(String datalog) {
        try {
            new LogicSchemaWithIDsParser().parse(datalog);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Merged Datalog is invalid after LLM enrichment", ex);
        }
    }
}
