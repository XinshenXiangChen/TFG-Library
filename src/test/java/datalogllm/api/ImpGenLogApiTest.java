package datalogllm.api;

import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImpGenLogApiTest {

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
            """;

    @TempDir
    Path outputDir;

    @Test
    void eachFacadeMethodProducesExpectedArtifact() throws Exception {
        ImpGenLogApi.umlToDatalog(UML, outputDir);
        assertThat(outputDir.resolve("schema.dl")).exists();

        ImpGenLogApi.umlToSql(UML, outputDir);
        assertThat(outputDir.resolve("schema.sql")).exists();

        ImpGenLogApi.umlToJava(UML, outputDir, "demo");
        assertThat(Files.walk(outputDir.resolve("java"))
                .anyMatch(p -> p.getFileName().toString().equals("Person.java")))
                .isTrue();

        ImpGenLogApi.generateLivelinessTests(UML, outputDir, "demo.liveness");
        assertThat(Files.walk(outputDir.resolve("liveness-tests"))
                .anyMatch(p -> p.getFileName().toString().endsWith("LivelinessTest.java")))
                .isTrue();

        ImpGenLogApi.generateSqlAssertions(outputDir);
        assertThat(Files.readString(outputDir.resolve("schema-assertions.sql")))
                .containsIgnoringCase("create assertion");
    }
}
