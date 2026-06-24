package datalogllm.uml.pipeline.clinic;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end structural pipeline for the mid-complexity clinic diagram.
 */
class StructuralFullPipelineClinicTest {

    @Test
    void structuralPipeline_generatesDatalogJavaSqlAndLiveliness(@TempDir Path outputDir) throws IOException {
        PlantUmlPipeline.generateDatalogAndJson(ClinicDiagramFixtures.STRUCTURAL_PLANTUML, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(
                ClinicDiagramFixtures.STRUCTURAL_PLANTUML,
                outputDir,
                "generated",
                "generated.liveness");

        String datalog = Files.readString(outputDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        assertThat(datalog)
                .contains("Person(")
                .contains("Patient(")
                .contains("Doctor(")
                .contains("Department(")
                .contains("Appointment(")
                .contains("Treatment(")
                .contains("InsurancePolicy(")
                .contains("employs(")
                .contains("attends(")
                .contains("schedules(")
                .contains("receives(")
                .contains("prescribes(")
                .contains("coveredBy(")
                .contains("isPersonAux(")
                .doesNotContain("%+ OCL constraints (LLM)");

        Path javaRoot = outputDir.resolve("java").resolve("generated");
        assertThat(Files.readString(javaRoot.resolve("Doctor.java"), StandardCharsets.UTF_8))
                .contains("public class Doctor extends Person")
                .contains("private String medicalLicense;");
        assertThat(Files.readString(javaRoot.resolve("Department.java"), StandardCharsets.UTF_8))
                .contains("private Integer id;")
                .contains("private String name;");
        assertThat(Files.readString(javaRoot.resolve("Treatment.java"), StandardCharsets.UTF_8))
                .contains("private Doctor doctor;")
                .contains("private Patient patient;");
        assertThat(Files.readString(javaRoot.resolve("InsurancePolicy.java"), StandardCharsets.UTF_8))
                .contains("private Patient patient;");

        Path livenessRoot = outputDir.resolve("liveness-tests").resolve("generated").resolve("liveness");
        assertThat(livenessRoot.resolve("DepartmentLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("TreatmentLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("InsurancePolicyLivelinessTest.java")).exists();

        String sql = Files.readString(outputDir.resolve("schema.sql"), StandardCharsets.UTF_8);
        assertThat(sql)
                .contains("CREATE TABLE Department")
                .contains("CREATE TABLE Treatment")
                .contains("CREATE TABLE InsurancePolicy")
                .contains("CREATE TABLE employs")
                .contains("CREATE TABLE receives")
                .contains("CREATE TABLE prescribes")
                .contains("CREATE TABLE coveredBy");
    }
}
