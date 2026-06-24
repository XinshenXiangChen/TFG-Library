package datalogllm.uml.pipeline.patients;

/**
 * Minimal patients-domain PlantUML for baseline pipeline tests.
 * For a richer diagram (extra classes, associations and cardinalities), see {@code clinic.ClinicDiagramFixtures}.
 */
public final class PatientsDiagramFixtures {

    private PatientsDiagramFixtures() {}

    /** Structural diagram only (no natural-language constraints after {@code @enduml}). */
    public static final String STRUCTURAL_PLANTUML = """
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

    /** Same model with business rules in natural language (LLM branch). */
    public static final String WITH_NL_CONSTRAINTS = STRUCTURAL_PLANTUML + """

            Appointment id must be greater than 0
            Appointment date must be greater than 20200101
            A doctor cannot attend two appointments on the same date
            """;
}
