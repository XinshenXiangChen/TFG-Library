package datalogllm.uml.pipeline.metamodel;

import datalogllm.pipeline.PlantUmlPipeline;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
@Disabled
public class MetamodelPlantUmlFullPipelineTest {

    private static final String PLANTUML_METAMODEL = """
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

            Appointment id must be greater than 0
            Appointment date must be greater than 20200101
            A doctor cannot attend two appointments on the same date
            """;

    private static final String EXPECTED_SCHEMA_SQL = """
            CREATE TABLE Person (
                nif VARCHAR(255),
                name VARCHAR(255)
            );
            
            CREATE TABLE Patient (
                nif VARCHAR(255),
                healthCardId VARCHAR(255)
            );
            
            CREATE TABLE Staff (
                nif VARCHAR(255),
                employeeId VARCHAR(255)
            );
            
            CREATE TABLE Doctor (
                nif VARCHAR(255),
                medicalLicense VARCHAR(255)
            );
            
            CREATE TABLE Nurse (
                nif VARCHAR(255),
                shift VARCHAR(255)
            );
            
            CREATE TABLE Appointment (
                id VARCHAR(255),
                date VARCHAR(255),
                PRIMARY KEY (id)
            );
            
            CREATE TABLE attends (
                nif INT,
                id INT
            );
            
            CREATE TABLE schedules (
                nif INT,
                id INT
            );
            """;

    @Test
    void fullPipeline_generatesAllArtifacts_forMetamodelExample() throws IOException {
        Path outputDir = Path.of("target", "generated", "metamodel");

        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(
            PLANTUML_METAMODEL,
            outputDir
        );
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(
            PLANTUML_METAMODEL,
            outputDir,
            "generated",
            "generated.liveness"
        );

        Path schemaDL = outputDir.resolve("schema.dl");
        verifyLivelinessTests(outputDir);
        verifySchemaDL(schemaDL);
        verifySchemaSQL(outputDir.resolve("schema.sql"));
        verifyGeneratedJavaFiles(outputDir.resolve("java").resolve("generated"));
    }

    private void verifyLivelinessTests(Path outputDir) {
        Path livenessRoot = outputDir.resolve("liveness-tests").resolve("generated").resolve("liveness");
        assertThat(livenessRoot.resolve("PersonLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("PatientLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("DoctorLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("NurseLivelinessTest.java")).exists();
        assertThat(livenessRoot.resolve("AppointmentLivelinessTest.java")).exists();
    }

    private void verifySchemaDL(Path path) throws IOException {
        assertThat(path).exists();
        String datalog = Files.readString(path, StandardCharsets.UTF_8);
        assertThat(datalog).contains("attends(");
        assertThat(datalog).contains("%  schedules(P0, A0)");
        assertThat(datalog).contains("isDoctorAux(D0) :- Doctor(D0, D1)");
        assertThat(datalog).contains("@1 :- Patient(P0, P1), not(isPersonAux(P0))");
        assertThat(datalog).contains("@5 :- attends(D0, A0), not(isDoctorAux(D0))");
        assertThat(datalog).contains("@6 :- attends(D0, A0), not(isAppointmentAux(A0))");
        assertThat(datalog).contains("@7 :- schedules(P0, A0), not(isPatientAux(P0))");
        assertThat(datalog).contains("@8 :- schedules(P0, A0), not(isAppointmentAux(A0))");
        assertThat(datalog).contains("@9 :- Appointment(A0, A1), not(Minattends_Doctor(A0))");
        assertThat(datalog).contains("@10 :- attends(L0, A0), attends(L1, A0), L1<>L0");
        assertThat(datalog).contains("@11 :- Appointment(A0, A1), not(Minschedules_Patient(A0))");
        assertThat(datalog).contains("@12 :- schedules(L0, A0), schedules(L1, A0), L1<>L0");
        assertThat(datalog).contains("%+ OCL constraints (LLM)");
        // Gemini output may vary in exact syntax; assert that LLM constraints were appended.
        assertThat(datalog).contains("@13");
        assertThat(datalog).contains("Appointment(");
    }

    private void verifySchemaSQL(Path path) throws IOException {
        assertThat(path).exists();
        String sql = Files.readString(path, StandardCharsets.UTF_8).trim();
        assertThat(sql).isEqualTo(EXPECTED_SCHEMA_SQL.trim());
    }

    private void verifyGeneratedJavaFiles(Path javaRoot) throws IOException {
        Path appointment = javaRoot.resolve("Appointment.java");
        Path person = javaRoot.resolve("Person.java");
        Path doctor = javaRoot.resolve("Doctor.java");
        Path patient = javaRoot.resolve("Patient.java");

        assertThat(appointment).exists();
        assertThat(person).exists();
        assertThat(doctor).exists();
        assertThat(patient).exists();

        String appointmentCode = Files.readString(appointment, StandardCharsets.UTF_8);
        assertThat(appointmentCode).contains("private String id;");
        assertThat(appointmentCode).contains("private String date;");
        assertThat(appointmentCode).contains("private Doctor doctor;");
        assertThat(appointmentCode).contains("private Patient patient;");

        String personCode = Files.readString(person, StandardCharsets.UTF_8);
        assertThat(personCode).contains("private String nif;");
        assertThat(personCode).contains("private String name;");

        String doctorCode = Files.readString(doctor, StandardCharsets.UTF_8);
        assertThat(doctorCode).contains("private String nif;");
        assertThat(doctorCode).contains("private String medicalLicense;");
        assertThat(doctorCode).contains("private List<Appointment> appointments;");

        String patientCode = Files.readString(patient, StandardCharsets.UTF_8);
        assertThat(patientCode).contains("private String nif;");
        assertThat(patientCode).contains("private String healthCardId;");
        assertThat(patientCode).contains("private List<Appointment> appointments;");
    }
}
