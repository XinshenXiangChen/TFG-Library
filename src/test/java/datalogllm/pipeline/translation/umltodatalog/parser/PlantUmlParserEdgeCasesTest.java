package datalogllm.pipeline.translation.umltodatalog.parser;

import datalogllm.pipeline.umlMetamodel.Association;
import datalogllm.pipeline.umlMetamodel.Generalization;
import datalogllm.pipeline.umlMetamodel.UmlAttribute;
import datalogllm.pipeline.umlMetamodel.UmlClass;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlantUmlParserEdgeCasesTest {

    private final PlantUmlParser parser = new PlantUmlParser();

    @Test
    void parse_nullInput_throws() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("plantUml");
    }

    @Test
    void parse_stripsLineCommentsBeforeParsing() {
        String plantUml = """
                @startuml
                class Person {
                  + String nif ' inline comment
                }
                @enduml
                """;

        UmlModel model = parser.parse(plantUml);
        UmlClass person = model.getClasses().stream()
                .filter(c -> "Person".equals(c.getClassName()))
                .findFirst()
                .orElseThrow();

        assertThat(person.getAttributes())
                .extracting(UmlAttribute::getName)
                .containsExactly("nif");
    }

    @Test
    void parse_attributeWithColonSyntax_parsesNameAndType() {
        String plantUml = """
                @startuml
                class Widget {
                  id: Integer
                }
                @enduml
                """;

        UmlClass widget = parser.parse(plantUml).getClasses().get(0);
        assertThat(widget.getAttributes()).hasSize(1);
        assertThat(widget.getAttributes().get(0).getName()).isEqualTo("id");
        assertThat(widget.getAttributes().get(0).getType()).isEqualTo("Integer");
    }

    @Test
    void parse_generalization_createsGeneralizationLink() {
        String plantUml = """
                @startuml
                class Animal {
                }
                class Dog {
                  + String name
                }
                Animal <|-- Dog
                @enduml
                """;

        UmlModel model = parser.parse(plantUml);
        assertThat(model.getGeneralizations()).hasSize(1);
        Generalization gen = model.getGeneralizations().get(0);
        assertThat(gen.getSuperClass().getClassName()).isEqualTo("Animal");
        assertThat(gen.getSubClass().getClassName()).isEqualTo("Dog");
    }

    @Test
    void parse_associationWithLabel_usesLabelAsAssociationName() {
        String plantUml = """
                @startuml
                class Left {
                  + Integer id
                }
                class Right {
                  + Integer id
                }
                Left "1" -- "0..*" Right : worksWith
                @enduml
                """;

        UmlModel model = parser.parse(plantUml);
        assertThat(model.getAssociations()).hasSize(1);
        Association association = model.getAssociations().get(0);
        assertThat(association.getName()).isEqualTo("worksWith");
        assertThat(association.getEnds()).hasSize(2);
    }

    @Test
    void parse_singleLetterClassNamesInAssociations_areIgnored() {
        String plantUml = """
                @startuml
                class Person {
                  + String nif
                }
                class Ticket {
                  + Integer id
                }
                A -- B
                Person "1" -- "0..*" Ticket : opens
                @enduml
                """;

        UmlModel model = parser.parse(plantUml);
        assertThat(model.getClasses())
                .extracting(UmlClass::getClassName)
                .containsExactlyInAnyOrder("Person", "Ticket");
        assertThat(model.getAssociations()).hasSize(1);
        assertThat(model.getAssociations().get(0).getName()).isEqualTo("opens");
    }
}
