package datalogllm.uml.pipeline.patients;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end pipeline over the patients diagram without calling an external LLM.
 * Mirrors {@link FullPipelinePatientsTest} but uses only structural Datalog.
 */
class StructuralFullPipelinePatientsTest {

    @Test
    void structuralPipeline_generatesDatalogJavaSqlAndLiveliness(@TempDir Path outputDir) throws IOException {
        PlantUmlPipeline.generateDatalogAndJson(PatientsDiagramFixtures.STRUCTURAL_PLANTUML, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(
                PatientsDiagramFixtures.STRUCTURAL_PLANTUML,
                outputDir,
                "generated",
                "generated.liveness");

        Path datalogPath = outputDir.resolve("schema.dl");
        Path javaOut = outputDir.resolve("java");
        Path sqlPath = outputDir.resolve("schema.sql");

        assertThat(datalogPath).exists();
        String datalog = Files.readString(datalogPath, StandardCharsets.UTF_8);
        assertThat(datalog)
                .contains("Person(")
                .contains("Patient(")
                .contains("Doctor(")
                .contains("Appointment(")
                .contains("attends(")
                .contains("schedules(")
                .doesNotContain("%+ OCL constraints (LLM)");

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

        Path appointmentJava = javaOut.resolve("generated").resolve("Appointment.java");
        assertThat(appointmentJava).exists();
        String appointmentCode = Files.readString(appointmentJava, StandardCharsets.UTF_8);
        assertThat(appointmentCode)
                .contains("private Integer id;")
                .contains("private Doctor doctor;")
                .contains("private Patient patient;");

        Path livelinessRoot = outputDir.resolve("liveness-tests")
                .resolve("generated")
                .resolve("liveness");
        assertThat(livelinessRoot).isDirectory();
        assertThat(livelinessRoot.resolve("PersonLivelinessTest.java")).exists();
        assertThat(livelinessRoot.resolve("PatientLivelinessTest.java")).exists();
        assertThat(livelinessRoot.resolve("DoctorLivelinessTest.java")).exists();
        assertThat(livelinessRoot.resolve("AppointmentLivelinessTest.java")).exists();

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
