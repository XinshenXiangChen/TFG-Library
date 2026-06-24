package datalogllm.pipeline;

import datalogllm.pipeline.translation.umltodatalog.constraints.ConstraintGeneratorFactory;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineConfigTest {

    @Test
    void structuralDefaults_usesPlantUmlParserAndNoOpNlGenerator() {
        PipelineConfig config = PipelineConfig.structuralDefaults();

        assertThat(config.umlParser()).isInstanceOf(PlantUmlParser.class);
        assertThat(config.nlConstraintGenerator())
                .isSameAs(ConstraintGeneratorFactory.noOp());
    }

    @Test
    void constructor_rejectsNullUmlParser() {
        assertThatThrownBy(() -> new PipelineConfig(null, ConstraintGeneratorFactory.noOp()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("umlParser");
    }

    @Test
    void constructor_rejectsNullNlGenerator() {
        assertThatThrownBy(() -> new PipelineConfig(UmlParser.defaultParser(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("nlConstraintGenerator");
    }

    @Test
    void withUmlParser_replacesParserOnly() {
        UmlParser customParser = umlText -> UmlParser.defaultParser().parse(umlText);
        PipelineConfig original = PipelineConfig.structuralDefaults();

        PipelineConfig updated = original.withUmlParser(customParser);

        assertThat(updated.umlParser()).isSameAs(customParser);
        assertThat(updated.nlConstraintGenerator()).isSameAs(original.nlConstraintGenerator());
    }

    @Test
    void withNlConstraintGenerator_replacesGeneratorOnly() {
        NaturalLanguageConstraintGenerator custom = (s, nl) -> "@99 :- stub(X)";
        PipelineConfig original = PipelineConfig.structuralDefaults();

        PipelineConfig updated = original.withNlConstraintGenerator(custom);

        assertThat(updated.nlConstraintGenerator()).isSameAs(custom);
        assertThat(updated.umlParser()).isSameAs(original.umlParser());
    }
}
