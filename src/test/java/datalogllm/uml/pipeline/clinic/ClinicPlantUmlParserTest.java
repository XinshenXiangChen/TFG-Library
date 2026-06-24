package datalogllm.uml.pipeline.clinic;

import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicPlantUmlParserTest {

    private final UmlParser parser = UmlParser.defaultParser();

    @Test
    void parse_clinicDiagram_buildsExtendedModel() {
        UmlModel model = parser.parse(ClinicDiagramFixtures.STRUCTURAL_PLANTUML);

        assertThat(model.getClasses()).hasSize(7);
        assertThat(model.getAssociations()).hasSize(6);
        assertThat(model.getGeneralizations()).hasSize(2);
        assertThat(model.getClasses().stream().map(c -> c.getClassName()))
                .containsExactlyInAnyOrder(
                        "Person", "Patient", "Doctor", "Department",
                        "Appointment", "Treatment", "InsurancePolicy");
    }

    @Test
    void parse_withNlConstraints_extractsNaturalLanguageBlock() {
        UmlModel model = parser.parse(ClinicDiagramFixtures.WITH_NL_CONSTRAINTS);

        assertThat(model.getNaturalLanguageConstraints())
                .contains("Appointment id must be greater than 0")
                .contains("Treatment description must not be empty");
    }
}
