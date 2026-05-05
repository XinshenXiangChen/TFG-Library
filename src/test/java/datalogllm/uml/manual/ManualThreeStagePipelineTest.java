package datalogllm.uml.manual;

import datalogllm.pipeline.PlantUmlPipeline;

import java.nio.file.Path;

/**
 * Manual runner for the 3-stage pipeline:
 * 1) UML -> Datalog (with natural language constraints)
 * 2) UML metamodel -> Java + IMP reasoner liveliness tests
 * 3) UML metamodel -> SQL + BLAST assertions
 */
public final class ManualThreeStagePipelineTest {

    private static final String PATIENTS_UML = """
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

    private ManualThreeStagePipelineTest() {
    }

    public static void main(String[] args) {
        Path outputDir = Path.of("target", "generated");
        String javaPackage = "generated";
        String livelinessPackage = "generated.liveness";

        System.out.println("=== Stage 1: UML -> Datalog (with natural language constraints) ===");
        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(PATIENTS_UML, outputDir);
        System.out.println("  - " + outputDir.resolve("schema.dl").toAbsolutePath());

        System.out.println("\n=== Stage 2 and 3: UML metamodel -> Java + SQL + tests/assertions ===");
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(PATIENTS_UML, outputDir, javaPackage, livelinessPackage);
        System.out.println("  - " + outputDir.resolve("java").toAbsolutePath());
        System.out.println("  - " + outputDir.resolve("liveness-tests").toAbsolutePath());
        System.out.println("  - " + outputDir.resolve("schema.sql").toAbsolutePath());
        System.out.println("  - " + outputDir.resolve("schema-assertions.sql").toAbsolutePath());
    }
}
