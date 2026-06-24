package datalogllm.uml.pipeline.clinic;

/**
 * Richer healthcare-domain PlantUML used as a mid-complexity pipeline example:
 * more classes and associations than {@code patients}, without the Staff/Nurse hierarchy of {@code metamodel}.
 */
public final class ClinicDiagramFixtures {

    private ClinicDiagramFixtures() {}

    /** Structural clinic diagram (no natural-language constraints after {@code @enduml}). */
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

            class Department {
              + Integer id
              + String name
            }

            class Appointment {
              + Integer id
              + Date date
            }

            class Treatment {
              + Integer id
              + String description
            }

            class InsurancePolicy {
              + String policyNumber
              + String provider
            }

            Person <|-- Patient
            Person <|-- Doctor

            Department "1" -- "1..*" Doctor : employs
            Doctor "1" -- "0..*" Appointment : attends
            Patient "1" -- "0..*" Appointment : schedules
            Patient "1" -- "0..*" Treatment : receives
            Doctor "0..1" -- "0..*" Treatment : prescribes
            Patient "1" -- "0..1" InsurancePolicy : coveredBy
            @enduml
            """;

    /** Same model with business rules in natural language (LLM branch). */
    public static final String WITH_NL_CONSTRAINTS = STRUCTURAL_PLANTUML + """

            Appointment id must be greater than 0
            Treatment description must not be empty
            """;
}
