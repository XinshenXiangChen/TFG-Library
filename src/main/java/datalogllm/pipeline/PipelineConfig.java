package datalogllm.pipeline;

import datalogllm.pipeline.translation.umltodatalog.constraints.ConstraintGeneratorFactory;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;

import java.util.Objects;

/**
 * Holds the interchangeable strategies used by the translation pipeline:
 * {@link UmlParser} for reading external UML text and
 * {@link NaturalLanguageConstraintGenerator} for NL constraint enrichment.
 */
public record PipelineConfig(
        UmlParser umlParser,
        NaturalLanguageConstraintGenerator nlConstraintGenerator) {

    public PipelineConfig {
        Objects.requireNonNull(umlParser, "umlParser must not be null");
        Objects.requireNonNull(nlConstraintGenerator, "nlConstraintGenerator must not be null");
    }

    /** PlantUML parser + no NL enrichment (structural Datalog path). */
    public static PipelineConfig structuralDefaults() {
        return new PipelineConfig(
                UmlParser.defaultParser(),
                ConstraintGeneratorFactory.noOp());
    }

    public PipelineConfig withUmlParser(UmlParser parser) {
        return new PipelineConfig(parser, nlConstraintGenerator);
    }

    public PipelineConfig withNlConstraintGenerator(NaturalLanguageConstraintGenerator generator) {
        return new PipelineConfig(umlParser, generator);
    }
}
