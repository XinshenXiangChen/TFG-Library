package datalogllm.pipeline.umlMetamodel.services.java;

import datalogllm.pipeline.PipelineConfig;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;

import java.nio.file.Path;
import java.util.Objects;

public final class UmlToJavaTranslationService {

    private final UmlParser umlParser;

    public UmlToJavaTranslationService() {
        this(UmlParser.defaultParser());
    }

    public UmlToJavaTranslationService(UmlParser umlParser) {
        this.umlParser = Objects.requireNonNull(umlParser, "umlParser must not be null");
    }

    public UmlToJavaTranslationService(PipelineConfig config) {
        this(config.umlParser());
    }

    public void translate(String plantUml, Path outputDirectory, String javaPackage, String livelinessTestPackage) {
        translateJavaClasses(plantUml, outputDirectory, javaPackage);
        translateLivelinessTests(plantUml, outputDirectory, livelinessTestPackage);
    }

    /** Generates Java POJOs only (no liveliness tests). */
    public void translateJavaClasses(String plantUml, Path outputDirectory, String javaPackage) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        String effectiveJavaPackage = javaPackage == null ? "" : javaPackage;

        UmlModel model = umlParser.parse(plantUml);
        new UmlModelToJavaGenerator(effectiveJavaPackage, outputDirectory.resolve("java")).generate(model);
    }

    /**
     * Generates IMP reasoner liveliness tests. Requires {@code schema.dl} in {@code outputDirectory}
     * (run {@link #translateJavaClasses} or Datalog translation first).
     */
    public void translateLivelinessTests(String plantUml, Path outputDirectory, String livelinessTestPackage) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        UmlModel model = umlParser.parse(plantUml);
        new JavaLivelinessTestService().generate(model, outputDirectory, livelinessTestPackage);
    }
}
