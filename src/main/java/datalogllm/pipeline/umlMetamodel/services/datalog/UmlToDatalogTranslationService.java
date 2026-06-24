package datalogllm.pipeline.umlMetamodel.services.datalog;

import datalogllm.pipeline.PipelineConfig;
import datalogllm.pipeline.translation.umltodatalog.constraints.ConstraintGeneratorFactory;
import datalogllm.pipeline.translation.umltodatalog.constraints.DatalogConstraintMerger;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.mapping.UmlToLogicSchemaMapper;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.translation.umltodatalog.utils.UmlToDatalogUtils;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.parser.LogicSchemaWithIDsParser;

import java.nio.file.Path;
import java.util.Objects;

/**
 * UML metamodel translation to Datalog ({@code schema.dl}), aligned with
 * {@link datalogllm.pipeline.umlMetamodel.services.java.UmlToJavaTranslationService}
 * and {@link datalogllm.pipeline.umlMetamodel.services.sql.UmlToSqlTranslationService}.
 */
public final class UmlToDatalogTranslationService {

    private final UmlParser umlParser;
    private final NaturalLanguageConstraintGenerator defaultNlGenerator;

    public UmlToDatalogTranslationService() {
        this(PipelineConfig.structuralDefaults());
    }

    public UmlToDatalogTranslationService(PipelineConfig config) {
        this(config.umlParser(), config.nlConstraintGenerator());
    }

    public UmlToDatalogTranslationService(UmlParser umlParser,
                                          NaturalLanguageConstraintGenerator defaultNlGenerator) {
        this.umlParser = Objects.requireNonNull(umlParser, "umlParser must not be null");
        this.defaultNlGenerator = Objects.requireNonNull(defaultNlGenerator, "defaultNlGenerator must not be null");
    }

    public UmlToDatalogTranslationService(UmlParser umlParser) {
        this(umlParser, ConstraintGeneratorFactory.noOp());
    }

    public void translate(String plantUml, Path outputDirectory) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        UmlModel model = umlParser.parse(plantUml);
        UmlToLogicSchemaMapper mapper = new UmlToLogicSchemaMapper();
        UmlToDatalogUtils.writeLogicSchema(outputDirectory.resolve("schema.dl"), mapper.toLogicSchema(model));
    }

    public void translateWithLlmConstraints(String plantUml, Path outputDirectory) {
        translateWithLlmConstraints(plantUml, outputDirectory, ConstraintGeneratorFactory.fromEnvironment());
    }

    public void translateWithLlmConstraints(String plantUml,
                                            Path outputDirectory,
                                            String geminiApiKey) {
        translateWithLlmConstraints(plantUml, outputDirectory, ConstraintGeneratorFactory.fromApiKey(geminiApiKey));
    }

    public void translateWithLlmConstraints(String plantUml,
                                            Path outputDirectory,
                                            NaturalLanguageConstraintGenerator constraintGenerator) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(constraintGenerator, "constraintGenerator must not be null");

        UmlModel model = umlParser.parse(plantUml);
        UmlToLogicSchemaMapper mapper = new UmlToLogicSchemaMapper();
        String structuralDatalog = mapper.toDatalog(model);
        String nlConstraints = model.getNaturalLanguageConstraints();
        String llmDatalog = constraintGenerator.generateConstraints(structuralDatalog, nlConstraints);
        String mergedDatalog = DatalogConstraintMerger.merge(structuralDatalog, llmDatalog);
        validateDatalog(mergedDatalog);

        UmlToDatalogUtils.write(outputDirectory.resolve("schema.dl"), mergedDatalog);
    }

    /** Uses the default NL strategy configured at construction (often {@link ConstraintGeneratorFactory#noOp()}). */
    public void translateWithDefaultNlStrategy(String plantUml, Path outputDirectory) {
        translateWithLlmConstraints(plantUml, outputDirectory, defaultNlGenerator);
    }

    private static void validateDatalog(String datalog) {
        try {
            new LogicSchemaWithIDsParser().parse(datalog);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Merged Datalog is invalid after LLM enrichment", ex);
        }
    }
}
