package datalogllm.uml.pipeline.patients;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UmlToJsonThenUmlMetamodelToJavaAndSqlTest {

    private static final String UML = PatientsDiagramFixtures.STRUCTURAL_PLANTUML;

    @Test
    void umlToDatalogWithNlConstraints_thenUmlMetamodelToJavaAndSql() throws IOException {
        Path outputDir = Path.of("target", "generated");
        Files.createDirectories(outputDir);
        Path javaOut = outputDir.resolve("java");
        Path sqlPath = outputDir.resolve("schema.sql");

        // Generate Datalog (with natural language constraints) from UML.
        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(UML, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(UML, outputDir, "generated", "generated.liveness");

        Path datalogPath = outputDir.resolve("schema.dl");
        String datalog = Files.readString(datalogPath, StandardCharsets.UTF_8);

        // Verify translation 1
        assertThat(datalogPath).exists();
        assertThat(datalog)
            .contains("@")
            .contains("Doctor")
            .contains("Person");

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
