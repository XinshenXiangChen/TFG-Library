package datalogllm.uml.pipeline.parser;

import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import datalogllm.uml.pipeline.patients.PatientsDiagramFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantUmlParserJUnitTest {

    private final UmlParser parser = UmlParser.defaultParser();

    @Test
    void parse_patientsDiagram_buildsExpectedClassesAndGeneralizations() {
        UmlModel model = parser.parse(PatientsDiagramFixtures.STRUCTURAL_PLANTUML);

        assertThat(model.getClasses())
                .extracting(c -> c.getClassName())
                .containsExactlyInAnyOrder("Person", "Patient", "Doctor", "Appointment");

        assertThat(model.getGeneralizations()).hasSize(2);
        assertThat(model.getAssociations()).hasSize(2);
        assertThat(model.getNaturalLanguageConstraints()).isBlank();
    }

    @Test
    void parse_withNlConstraintsAfterEnduml_capturesNaturalLanguageBlock() {
        UmlModel model = parser.parse(PatientsDiagramFixtures.WITH_NL_CONSTRAINTS);

        assertThat(model.getNaturalLanguageConstraints())
                .contains("Appointment id must be greater than 0")
                .contains("same date");
    }
}