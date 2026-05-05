package datalogllm.pipeline.umlMetamodel.services.java;

import datalogllm.pipeline.umlMetamodel.services.java.liveliness.LivelinessTestGenerator;
import datalogllm.pipeline.umlMetamodel.UmlModel;

import java.nio.file.Path;
import java.util.Objects;

final class JavaLivelinessTestService {
    private static final String DEFAULT_LIVELINESS_PACKAGE = "generated.liveness";

    void generate(UmlModel model, Path outputDirectory, String livelinessTestPackage) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        String effectiveLivelinessPackage =
            (livelinessTestPackage == null || livelinessTestPackage.isBlank())
                ? DEFAULT_LIVELINESS_PACKAGE
                : livelinessTestPackage;
        Path schemaFile = outputDirectory.resolve("schema.dl");
        Path livelinessDir = outputDirectory.resolve("liveness-tests");
        new LivelinessTestGenerator(effectiveLivelinessPackage, livelinessDir).generate(model, schemaFile);
    }
}
