package datalogllm.pipeline.translation.umltodatalog;

import datalogllm.pipeline.PipelineConfig;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.umlMetamodel.services.datalog.UmlToDatalogTranslationService;

import java.nio.file.Path;

/**
 * Legacy entry point for UML {@code ->} Datalog. Prefer
 * {@link UmlToDatalogTranslationService#translate(String, java.nio.file.Path)}.
 */
public final class UmlToDatalog {

    private static final UmlToDatalogTranslationService SERVICE =
            new UmlToDatalogTranslationService(PipelineConfig.structuralDefaults());

    private UmlToDatalog() {
    }

    public static void generate(String plantUml, Path outputDirectory) {
        SERVICE.translate(plantUml, outputDirectory);
    }

    public static void generateWithLlmConstraints(String plantUml,
                                                  Path outputDirectory,
                                                  NaturalLanguageConstraintGenerator constraintGenerator) {
        SERVICE.translateWithLlmConstraints(plantUml, outputDirectory, constraintGenerator);
    }

    public static void generateWithLlmConstraints(String plantUml, Path outputDirectory) {
        SERVICE.translateWithLlmConstraints(plantUml, outputDirectory);
    }

    public static void generateWithLlmConstraints(String plantUml, Path outputDirectory, String geminiApiKey) {
        SERVICE.translateWithLlmConstraints(plantUml, outputDirectory, geminiApiKey);
    }
}
