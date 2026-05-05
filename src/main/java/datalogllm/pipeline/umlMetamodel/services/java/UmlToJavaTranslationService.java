package datalogllm.pipeline.umlMetamodel.services.java;

import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;

import java.nio.file.Path;
import java.util.Objects;

public final class UmlToJavaTranslationService {

    public void translate(String plantUml, Path outputDirectory, String javaPackage, String livelinessTestPackage) {
        Objects.requireNonNull(plantUml, "plantUml must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        String effectiveJavaPackage = javaPackage == null ? "" : javaPackage;

        UmlModel model = PlantUmlParser.parse(plantUml);
        new UmlModelToJavaGenerator(effectiveJavaPackage, outputDirectory.resolve("java")).generate(model);
        new JavaLivelinessTestService().generate(model, outputDirectory, livelinessTestPackage);
    }
}
