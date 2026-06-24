package datalogllm.pipeline.translation.umltodatalog.mapping;

import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UmlToLogicSchemaMapperTest {

    private final UmlToLogicSchemaMapper mapper = new UmlToLogicSchemaMapper();
    private final UmlParser parser = UmlParser.defaultParser();

    @Test
    void toSqlSchema_escapesSqlReservedClassNames() {
        String plantUml = """
                @startuml
                class User {
                  + String nif
                }
                @enduml
                """;

        String sql = mapper.toSqlSchema(parser.parse(plantUml));

        assertThat(sql).contains("CREATE TABLE User_");
        assertThat(sql).doesNotContain("CREATE TABLE User\n");
    }

    @Test
    void toSqlSchema_escapesDateColumnName() {
        String plantUml = """
                @startuml
                class Event {
                  + Integer id
                  + Date date
                }
                @enduml
                """;

        String sql = mapper.toSqlSchema(parser.parse(plantUml));

        assertThat(sql).contains("date_ INT");
    }

    @Test
    void toSqlSchema_infersPrimaryKeyFromIdAttribute() {
        String plantUml = """
                @startuml
                class Ticket {
                  + Integer id
                  + String code
                }
                @enduml
                """;

        String sql = mapper.toSqlSchema(parser.parse(plantUml));

        assertThat(sql).contains("PRIMARY KEY (id)");
    }

    @Test
    void toJsonSchema_includesExtendsForSubclasses() {
        String plantUml = """
                @startuml
                class Person {
                  + String nif
                }
                class Patient {
                  + String healthCardId
                }
                Person <|-- Patient
                @enduml
                """;

        JSONObject json = mapper.toJsonSchema(parser.parse(plantUml));

        assertThat(json.getJSONObject("Patient").getString("extends")).isEqualTo("Person");
        assertThat(json.getJSONObject("Person").has("extends")).isFalse();
    }

    @Test
    void toDatalog_emitsInheritanceConstraintsBeforeAssociationOnes() {
        String plantUml = """
                @startuml
                class Person {
                  + String nif
                }
                class Patient {
                  + String nif
                }
                class Doctor {
                  + String nif
                }
                class Appointment {
                  + String id
                }
                Person <|-- Patient
                Person <|-- Doctor
                Doctor "1" -- "0..*" Appointment : attends
                @enduml
                """;

        UmlModel model = parser.parse(plantUml);
        String datalog = mapper.toDatalog(model);

        assertThat(datalog.indexOf("@1 :- Patient(")).isLessThan(datalog.indexOf("@3 :- attends("));
        assertThat(datalog).contains("@2 :- Doctor(");
        assertThat(datalog).contains("Minattends_Doctor(");
    }

    @Test
    void toSqlSchema_addsForeignKeysWhenAssociationEndMultiplicityIsOne() {
        String plantUml = """
                @startuml
                class Doctor {
                  + String nif
                }
                class Appointment {
                  + Integer id
                }
                Doctor "1" -- "0..*" Appointment : attends
                @enduml
                """;

        String sql = mapper.toSqlSchema(parser.parse(plantUml));

        assertThat(sql).contains("CREATE TABLE attends");
        assertThat(sql).contains("FOREIGN KEY (nif) REFERENCES Doctor(nif)");
        assertThat(sql).doesNotContain("FOREIGN KEY (id) REFERENCES");
    }

    @Test
    void toSqlSchema_noForeignKeysWhenBothEndsAreMany() {
        String plantUml = """
                @startuml
                class Person {
                  + String nif
                }
                class Ticket {
                  + Integer id
                }
                Person "0..*" -- "0..*" Ticket : buys
                @enduml
                """;

        String sql = mapper.toSqlSchema(parser.parse(plantUml));

        assertThat(sql).contains("CREATE TABLE buys");
        assertThat(sql).doesNotContain("FOREIGN KEY");
    }
}
