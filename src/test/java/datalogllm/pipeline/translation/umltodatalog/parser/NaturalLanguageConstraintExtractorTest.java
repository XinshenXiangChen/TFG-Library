package datalogllm.pipeline.translation.umltodatalog.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaturalLanguageConstraintExtractorTest {

    @Test
    void extract_nullInput_throws() {
        assertThatThrownBy(() -> NaturalLanguageConstraintExtractor.extract(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("plantUml");
    }

    @Test
    void extract_noEndUmlMarker_returnsEmpty() {
        assertThat(NaturalLanguageConstraintExtractor.extract("@startuml\nclass A\n"))
                .isBlank();
    }

    @Test
    void extract_textAfterEnduml_joinsNonBlankLines() {
        String plantUml = """
                @startuml
                class A
                @enduml

                Rule one
                Rule two
                """;

        assertThat(NaturalLanguageConstraintExtractor.extract(plantUml))
                .isEqualTo("Rule one\nRule two");
    }

    @Test
    void extract_endUmlCaseInsensitive_stillCapturesConstraints() {
        String plantUml = """
                @startuml
                class A
                @ENDUML
                Constraint after marker
                """;

        assertThat(NaturalLanguageConstraintExtractor.extract(plantUml))
                .isEqualTo("Constraint after marker");
    }

    @Test
    void extract_ignoresBlankLinesAfterEnduml() {
        String plantUml = """
                @enduml


                Only this counts
                """;

        assertThat(NaturalLanguageConstraintExtractor.extract(plantUml))
                .isEqualTo("Only this counts");
    }

    @Test
    void extract_ignoresTextBeforeEnduml() {
        String plantUml = """
                @startuml
                not a constraint
                @enduml
                real constraint
                """;

        assertThat(NaturalLanguageConstraintExtractor.extract(plantUml))
                .isEqualTo("real constraint");
    }
}
