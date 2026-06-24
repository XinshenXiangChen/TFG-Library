package datalogllm.pipeline.translation.umltodatalog.mapping;

import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.domain.LogicSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UmlModelToLogicSchemaTest {

    private static final UmlParser PARSER = UmlParser.defaultParser();

    private static final String UML = """
            @startuml
            class Person {
              + String nif
            }
            class Patient {
              + String nif
            }
            Person <|-- Patient
            @enduml
            """;

    @Test
    void fromUmlModel_producesLogicSchemaWithConstraintsAndRules() {
        UmlModel model = PARSER.parse(UML);
        LogicSchema schema = UmlModelToLogicSchema.fromUmlModel(model);

        assertThat(schema.getAllLogicConstraints()).isNotEmpty();
        assertThat(schema.getAllDerivationRules()).isNotEmpty();
        assertThat(schema.getPredicateByName("Person").getArity()).isEqualTo(1);
        assertThat(schema.getPredicateByName("Patient").getArity()).isEqualTo(1);
    }

    @Test
    void fromUmlModel_addsSubclassConstraintForGeneralization() {
        UmlModel model = PARSER.parse(UML);
        LogicSchema schema = UmlModelToLogicSchema.fromUmlModel(model);

        assertThat(schema.getAllLogicConstraints()).anySatisfy(c -> {
            String text = c.toString();
            assertThat(text).contains("Patient");
            assertThat(text).contains("isPersonAux");
        });
    }

    @Test
    void fromUmlModel_definesAuxiliaryPredicateForSuperclass() {
        UmlModel model = PARSER.parse(UML);
        LogicSchema schema = UmlModelToLogicSchema.fromUmlModel(model);

        assertThat(schema.getPredicateByName("isPersonAux").getArity()).isEqualTo(1);
        assertThat(schema.getAllDerivationRules()).anySatisfy(r -> {
            String text = r.toString();
            assertThat(text).contains("isPersonAux");
            assertThat(text).contains("Person");
        });
    }
}
