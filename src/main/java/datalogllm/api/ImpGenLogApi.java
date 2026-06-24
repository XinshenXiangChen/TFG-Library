package datalogllm.api;

import datalogllm.pipeline.PipelineConfig;
import datalogllm.pipeline.umlMetamodel.services.datalog.UmlToDatalogTranslationService;
import datalogllm.pipeline.umlMetamodel.services.java.UmlToJavaTranslationService;
import datalogllm.pipeline.umlMetamodel.services.sql.UmlToSqlTranslationService;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Public facade for external integrators of IMP-GenLog.
 * <p>
 * Each method maps to one library use case. Call them independently in any order,
 * except that liveliness tests require {@code schema.dl} and SQL assertions require
 * both {@code schema.dl} and {@code schema.sql} in the output directory.
 * <p>
 * For a one-shot full pipeline (all artifacts), use {@link datalogllm.pipeline.PlantUmlPipeline#generate}.
 */
public final class ImpGenLogApi {

    private static final String DEFAULT_JAVA_PACKAGE = "generated";
    private static final String DEFAULT_LIVELINESS_PACKAGE = "generated.liveness";

    private ImpGenLogApi() {
    }

    /** Use case 1: PlantUML → structural Datalog ({@code schema.dl}). */
    public static void umlToDatalog(String plantUml, Path outputDirectory) {
        umlToDatalog(plantUml, outputDirectory, PipelineConfig.structuralDefaults());
    }

    public static void umlToDatalog(String plantUml, Path outputDirectory, PipelineConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        new UmlToDatalogTranslationService(config).translate(plantUml, outputDirectory);
    }

    /** Use case 2: PlantUML → Java POJOs under {@code outputDirectory/java/}. */
    public static void umlToJava(String plantUml, Path outputDirectory) {
        umlToJava(plantUml, outputDirectory, DEFAULT_JAVA_PACKAGE);
    }

    public static void umlToJava(String plantUml, Path outputDirectory, String javaPackage) {
        umlToJava(plantUml, outputDirectory, javaPackage, PipelineConfig.structuralDefaults());
    }

    public static void umlToJava(String plantUml, Path outputDirectory, String javaPackage, PipelineConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        new UmlToJavaTranslationService(config).translateJavaClasses(plantUml, outputDirectory, javaPackage);
    }

    /** Use case 3: PlantUML → SQL DDL ({@code schema.sql}). */
    public static void umlToSql(String plantUml, Path outputDirectory) {
        umlToSql(plantUml, outputDirectory, PipelineConfig.structuralDefaults());
    }

    public static void umlToSql(String plantUml, Path outputDirectory, PipelineConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        new UmlToSqlTranslationService(config).translateSqlSchema(plantUml, outputDirectory);
    }

    /**
     * Use case 4: PlantUML → IMP reasoner liveliness tests under {@code outputDirectory/liveness-tests/}.
     * Requires {@code schema.dl} in {@code outputDirectory}.
     */
    public static void generateLivelinessTests(String plantUml, Path outputDirectory) {
        generateLivelinessTests(plantUml, outputDirectory, DEFAULT_LIVELINESS_PACKAGE);
    }

    public static void generateLivelinessTests(String plantUml, Path outputDirectory, String livelinessTestPackage) {
        generateLivelinessTests(plantUml, outputDirectory, livelinessTestPackage, PipelineConfig.structuralDefaults());
    }

    public static void generateLivelinessTests(String plantUml,
                                               Path outputDirectory,
                                               String livelinessTestPackage,
                                               PipelineConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        new UmlToJavaTranslationService(config).translateLivelinessTests(
                plantUml, outputDirectory, livelinessTestPackage);
    }

    /**
     * Use case 5: Datalog + SQL → SQL assertions ({@code schema-assertions.sql}).
     * Requires {@code schema.dl} and {@code schema.sql} in {@code outputDirectory}.
     */
    public static void generateSqlAssertions(Path outputDirectory) {
        generateSqlAssertions(outputDirectory, PipelineConfig.structuralDefaults());
    }

    public static void generateSqlAssertions(Path outputDirectory, PipelineConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        new UmlToSqlTranslationService(config).translateSqlAssertions(outputDirectory);
    }
}
