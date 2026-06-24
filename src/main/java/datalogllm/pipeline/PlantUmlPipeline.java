package datalogllm.pipeline;

import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.umlMetamodel.services.datalog.UmlToDatalogTranslationService;
import datalogllm.pipeline.umlMetamodel.services.java.UmlToJavaTranslationService;
import datalogllm.pipeline.umlMetamodel.services.sql.UmlToSqlTranslationService;

import java.nio.file.Path;

/**
 * Pipeline split in three explicit translations from the UML metamodel:
 * 1) Datalog ({@link UmlToDatalogTranslationService})
 * 2) Java ({@link UmlToJavaTranslationService})
 * 3) SQL + assertions ({@link UmlToSqlTranslationService})
 * <p>
 * UML parsing and NL constraint generation are wired through {@link PipelineConfig}
 * strategy objects (see {@link datalogllm.pipeline.translation.umltodatalog.parser.UmlParser}
 * and {@link datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator}).
 */
public final class PlantUmlPipeline {

    private static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "generated");
    private static final String DEFAULT_JAVA_PACKAGE = "generated";
    private static final String DEFAULT_LIVELINESS_PACKAGE = "generated.liveness";
    private static final PipelineConfig DEFAULT_CONFIG = PipelineConfig.structuralDefaults();

    private static final UmlToDatalogTranslationService UML_TO_DATALOG_TRANSLATION_SERVICE =
            new UmlToDatalogTranslationService(DEFAULT_CONFIG);
    private static final UmlToJavaTranslationService UML_TO_JAVA_TRANSLATION_SERVICE =
            new UmlToJavaTranslationService(DEFAULT_CONFIG);
    private static final UmlToSqlTranslationService UML_TO_SQL_TRANSLATION_SERVICE =
            new UmlToSqlTranslationService(DEFAULT_CONFIG);

    private PlantUmlPipeline() {}

    public static void generate(String plantUml) {
        generate(plantUml, DEFAULT_OUTPUT_DIR, DEFAULT_JAVA_PACKAGE, DEFAULT_LIVELINESS_PACKAGE);
    }

    public static void generate(String plantUml,
                                Path outputDirectory,
                                String javaPackage,
                                String livelinessTestPackage) {
        generateDatalogFromUmlMetamodelWithLlmConstraints(plantUml, outputDirectory);
        generateJavaFromUmlMetamodel(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
        generateSqlAndAssertionsFromUmlMetamodel(plantUml, outputDirectory);
    }

    /** UML metamodel translation to Datalog ({@code schema.dl}), structural only. */
    public static void generateDatalogFromUmlMetamodel(String plantUml, Path outputDirectory) {
        UML_TO_DATALOG_TRANSLATION_SERVICE.translate(plantUml, outputDirectory);
    }

    /** UML metamodel translation to Datalog with LLM enrichment of constraints. */
    public static void generateDatalogFromUmlMetamodelWithLlmConstraints(String plantUml,
                                                                         Path outputDirectory,
                                                                         NaturalLanguageConstraintGenerator constraintGenerator) {
        UML_TO_DATALOG_TRANSLATION_SERVICE.translateWithLlmConstraints(
                plantUml, outputDirectory, constraintGenerator);
    }

    public static void generateDatalogFromUmlMetamodelWithLlmConstraints(String plantUml,
                                                                         Path outputDirectory) {
        UML_TO_DATALOG_TRANSLATION_SERVICE.translateWithLlmConstraints(plantUml, outputDirectory);
    }

    public static void generateDatalogFromUmlMetamodelWithLlmConstraints(String plantUml,
                                                                         Path outputDirectory,
                                                                         String geminiApiKey) {
        UML_TO_DATALOG_TRANSLATION_SERVICE.translateWithLlmConstraints(plantUml, outputDirectory, geminiApiKey);
    }

    /** Alias of {@link #generateDatalogFromUmlMetamodel(String, Path)}. */
    public static void generateDatalogAndJson(String plantUml, Path outputDirectory) {
        generateDatalogFromUmlMetamodel(plantUml, outputDirectory);
    }

    /** Alias of {@link #generateDatalogFromUmlMetamodelWithLlmConstraints(String, Path, NaturalLanguageConstraintGenerator)}. */
    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml,
                                                                Path outputDirectory,
                                                                NaturalLanguageConstraintGenerator constraintGenerator) {
        generateDatalogFromUmlMetamodelWithLlmConstraints(plantUml, outputDirectory, constraintGenerator);
    }

    /** Alias of {@link #generateDatalogFromUmlMetamodelWithLlmConstraints(String, Path)}. */
    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml, Path outputDirectory) {
        generateDatalogFromUmlMetamodelWithLlmConstraints(plantUml, outputDirectory);
    }

    /** Alias of {@link #generateDatalogFromUmlMetamodelWithLlmConstraints(String, Path, String)}. */
    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml,
                                                                Path outputDirectory,
                                                                String geminiApiKey) {
        generateDatalogFromUmlMetamodelWithLlmConstraints(plantUml, outputDirectory, geminiApiKey);
    }

    /** UML metamodel translation to Java + IMP reasoner liveliness tests. */
    public static void generateJavaFromUmlMetamodel(String plantUml,
                                                    Path outputDirectory,
                                                    String javaPackage,
                                                    String livelinessTestPackage) {
        UML_TO_JAVA_TRANSLATION_SERVICE.translate(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
    }

    /** UML metamodel translation to SQL + BLAST assertions. */
    public static void generateSqlAndAssertionsFromUmlMetamodel(String plantUml, Path outputDirectory) {
        UML_TO_SQL_TRANSLATION_SERVICE.translate(plantUml, outputDirectory);
    }

    /** Convenience call to run both UML metamodel translations (Java and SQL). */
    public static void generateJavaAndSqlFromUmlMetamodel(String plantUml,
                                                          Path outputDirectory,
                                                          String javaPackage,
                                                          String livelinessTestPackage) {
        generateJavaFromUmlMetamodel(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
        generateSqlAndAssertionsFromUmlMetamodel(plantUml, outputDirectory);
    }

    /** Builds translation services sharing the same {@link PipelineConfig} strategies. */
    public static TranslationServices services(PipelineConfig config) {
        return new TranslationServices(
                new UmlToDatalogTranslationService(config),
                new UmlToJavaTranslationService(config),
                new UmlToSqlTranslationService(config));
    }

    public record TranslationServices(
            UmlToDatalogTranslationService datalog,
            UmlToJavaTranslationService javaService,
            UmlToSqlTranslationService sql) {
    }
}
