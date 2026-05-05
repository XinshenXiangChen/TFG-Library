package datalogllm.uml.pipeline.patients;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FullPipelinePatientsTest {

    private static final String UML = """
            @startuml
            class Person {
              + String nif
              + String name
            }

            class Patient {
              + String nif
              + String healthCardId
            }

            class Doctor {
              + String nif
              + String medicalLicense
            }

            class Appointment {
              + Integer id
              + Date date
            }

            Person <|-- Patient
            Person <|-- Doctor
            Doctor "1" -- "0..*" Appointment : attends
            Patient "1" -- "0..*" Appointment : schedules
            @enduml

            Appointment id must be greater than 0
            Appointment date must be greater than 20200101
            A doctor cannot attend two appointments on the same date
            """;

    @Test
    void umlToDatalogWithNlConstraints_thenUmlMetamodelToJavaAndSql() throws IOException {
        Path outputDir = Path.of("target", "generated");
        Files.createDirectories(outputDir);

        // Two generation lines only
        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(UML, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(UML, outputDir, "generated", "generated.liveness");

        Path datalogPath = outputDir.resolve("schema.dl");
        Path javaOut = outputDir.resolve("java");
        Path sqlPath = outputDir.resolve("schema.sql");
        String datalog = Files.readString(datalogPath, StandardCharsets.UTF_8);

        // Verify translation 1
        assertThat(datalogPath).exists();
        assertThat(datalog)
                .contains("%+ Constraints for hierarchies")
                .contains("Doctor")
                .contains("Person")
                .contains("%+ OCL constraints (LLM)")
                .contains("@9");

        // Verify translation 2
        Path doctorJava = javaOut.resolve("generated").resolve("Doctor.java");
        assertThat(doctorJava).exists();
        String doctorCode = Files.readString(doctorJava, StandardCharsets.UTF_8);
        assertThat(doctorCode)
                .contains("public class Doctor extends Person")
                .contains("private String medicalLicense;");
        Path patientJava = javaOut.resolve("generated").resolve("Patient.java");
        assertThat(patientJava).exists();
        String patientCode = Files.readString(patientJava, StandardCharsets.UTF_8);
        assertThat(patientCode)
                .contains("public class Patient extends Person")
                .contains("private String healthCardId;");
        Path livelinessRoot = outputDir.resolve("liveness-tests")
                .resolve("generated")
                .resolve("liveness");
        assertThat(livelinessRoot).exists().isDirectory();
        assertThat(livelinessRoot.resolve("DoctorLivelinessTest.java")).exists();
        assertThat(livelinessRoot.resolve("PatientLivelinessTest.java")).exists();

        // Verify translation 3
        assertThat(sqlPath).exists();
        String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);
        assertThat(sql)
                .contains("CREATE TABLE Person")
                .contains("CREATE TABLE Patient")
                .contains("CREATE TABLE Doctor")
                .contains("CREATE TABLE Appointment")
                .contains("PRIMARY KEY (id)");
    }
}
