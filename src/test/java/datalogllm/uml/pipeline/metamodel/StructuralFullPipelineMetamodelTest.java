package datalogllm.uml.pipeline.metamodel;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural end-to-end test for the richer hospital staff metamodel (Patient, Nurse, Staff hierarchy).
 */
class StructuralFullPipelineMetamodelTest {

    private static final String METAMODEL_PLANTUML = """
            @startuml
            skinparam linetype ortho

            class Person {
              + String nif
              + String name
            }

            class Patient {
              + String nif
              + String healthCardId
            }

            class Staff {
              + String nif
              + String employeeId
            }

            class Doctor {
              + String nif
              + String medicalLicense
            }

            class Nurse {
              + String nif
              + String shift
            }

            Person <|-- Patient
            Person <|-- Staff
            Staff <|-- Doctor
            Staff <|-- Nurse

            class Appointment {
              + String id
              + String date
            }

            Doctor "1" -- "0..*" Appointment : attends
            Patient "1" -- "0..*" Appointment : schedules
            @enduml
            """;

    @Test
    void structuralPipeline_generatesAllArtifacts_forMetamodelExample(@TempDir Path outputDir) throws IOException {
        PlantUmlPipeline.generateDatalogAndJson(METAMODEL_PLANTUML, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(
                METAMODEL_PLANTUML,
                outputDir,
                "generated",
                "generated.liveness");

        String datalog = Files.readString(outputDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        assertThat(datalog)
                .contains("attends(")
                .contains("schedules(")
                .contains("isDoctorAux(")
                .contains("isPatientAux(")
                .contains("@1 :- Patient(")
                .contains("@5 :- attends(D0, A0), not(isDoctorAux(D0))")
                .contains("@9 :- Appointment(A0, A1), not(Minattends_Doctor(A0))");

        Path javaRoot = outputDir.resolve("java").resolve("generated");
        assertThat(javaRoot.resolve("Nurse.java")).exists();
        assertThat(javaRoot.resolve("Staff.java")).exists();
        assertThat(Files.readString(javaRoot.resolve("Appointment.java"), StandardCharsets.UTF_8))
                .contains("private Doctor doctor;")
                .contains("private Patient patient;");

        Path livenessRoot = outputDir.resolve("liveness-tests").resolve("generated").resolve("liveness");
        assertThat(livenessRoot.resolve("PersonLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("NurseLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("AppointmentLivelinessTest.java")).exists();

        String sql = Files.readString(outputDir.resolve("schema.sql"), StandardCharsets.UTF_8);
        assertThat(sql)
                .contains("CREATE TABLE Nurse")
                .contains("CREATE TABLE Staff")
                .contains("CREATE TABLE attends")
                .contains("CREATE TABLE schedules");
    }
}
