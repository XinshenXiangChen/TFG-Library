package datalogllm.pipeline;

import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.umlMetamodel.services.java.UmlToJavaTranslationService;
import datalogllm.pipeline.umlMetamodel.services.sql.UmlToSqlTranslationService;
import datalogllm.pipeline.translation.umltodatalog.UmlToDatalog;

import java.nio.file.Path;

/**
 * Pipeline split in three explicit translations:
 * 1) UmlToDatalog
 * 2) Uml metamodel -> Java
 * 3) Uml metamodel -> SQL + assertions
 */
public final class PlantUmlPipeline {

    private static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "generated");
    private static final String DEFAULT_JAVA_PACKAGE = "generated";
    private static final String DEFAULT_LIVELINESS_PACKAGE = "generated.liveness";
    private static final UmlToJavaTranslationService UML_TO_JAVA_TRANSLATION_SERVICE = new UmlToJavaTranslationService();
    private static final UmlToSqlTranslationService UML_TO_SQL_TRANSLATION_SERVICE = new UmlToSqlTranslationService();

    private PlantUmlPipeline() {}

    public static void generate(String plantUml) {
        generate(plantUml, DEFAULT_OUTPUT_DIR, DEFAULT_JAVA_PACKAGE, DEFAULT_LIVELINESS_PACKAGE);
    }

    public static void generate(String plantUml,
                                Path outputDirectory,
                                String javaPackage,
                                String livelinessTestPackage) {
        generateDatalogAndJsonWithLlmConstraints(plantUml, outputDirectory);
        generateJavaFromUmlMetamodel(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
        generateSqlAndAssertionsFromUmlMetamodel(plantUml, outputDirectory);
    }

    /** UmlToDatalog translation: parse UML and write {@code schema.dl}. */
    public static void generateDatalogAndJson(String plantUml, Path outputDirectory) {
        UmlToDatalog.generate(plantUml, outputDirectory);
    }

    /** UmlToDatalog translation with LLM enrichment of Datalog constraints. */
    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml,
                                                                Path outputDirectory,
                                                                NaturalLanguageConstraintGenerator constraintGenerator) {
        UmlToDatalog.generateWithLlmConstraints(plantUml, outputDirectory, constraintGenerator);
    }

    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml,
                                                                Path outputDirectory) {
        UmlToDatalog.generateWithLlmConstraints(plantUml, outputDirectory);
    }

    public static void generateDatalogAndJsonWithLlmConstraints(String plantUml,
                                                                Path outputDirectory,
                                                                String geminiApiKey) {
        UmlToDatalog.generateWithLlmConstraints(plantUml, outputDirectory, geminiApiKey);
    }

    /** UML metamodel translation to Java + IMP reasoner liveliness tests. */
    public static void generateJavaFromUmlMetamodel(String plantUml,
                                                    Path outputDirectory,
                                                    String javaPackage,
                                                    String livelinessTestPackage) {
        UML_TO_JAVA_TRANSLATION_SERVICE.translate(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
    }

    /** UML metamodel translation to SQL + BLAST assertions. */
    public static void generateSqlAndAssertionsFromUmlMetamodel(String plantUml,
                                                                Path outputDirectory) {
        UML_TO_SQL_TRANSLATION_SERVICE.translate(plantUml, outputDirectory);
    }

    /** Convenience call to run both UML metamodel translations. */
    public static void generateJavaAndSqlFromUmlMetamodel(String plantUml,
                                                           Path outputDirectory,
                                                           String javaPackage,
                                                           String livelinessTestPackage) {
        generateJavaFromUmlMetamodel(plantUml, outputDirectory, javaPackage, livelinessTestPackage);
        generateSqlAndAssertionsFromUmlMetamodel(plantUml, outputDirectory);
    }

}



