package datalogllm.uml.manual;

import datalogllm.pipeline.umlMetamodel.services.java.UmlToJavaTranslationService;

import java.nio.file.Path;

/**
 * Generates Java class files from a PlantUML diagram.
 * <p>
 * 1) Parses the PlantUML (User/Business example).
 * 2) Builds the UML metamodel.
 * 3) Generates Java from UML metamodel to target/generated/java.
 *
 * Run from IDE or:
 *   mvn -q test-compile
 *   java -cp target/test-classes;target/classes JavaFromPlantUmlTest
 */
public final class JavaFromPlantUmlTest {

    private static final String EXAMPLE = """
            @startuml
            skinparam linetype ortho
            skinparam classAttributeIconSize 0

            class Class {
                + String className
                ==
                is_class(ClassName)
            }

            class Attribute {
                + String name
                + String type
                ==
                has_attribute(Class, Name, Type)
            }

            class Association {
                + String id
                + String name
                ==
                association(ID, Name)
            }

            class AssociationEnd {
                + String roleName
                + int min
                + int max
                + boolean isNavigable
                ==
                assoc_end(AssocID, ClassName, Role, Min, Max, IsNav)
            }

            '--- Relaciones Estructurales ---

            Class "1" *-- "0..*" Attribute

            ' Relación de Asociación
            Association "1" *-- "2" AssociationEnd
            AssociationEnd "0..*" -- "1" Class : "apunta_a"
            @enduml
            """;

    public static void main(String[] args) {
        Path outputDir = Path.of("target", "generated");
        new UmlToJavaTranslationService().translate(EXAMPLE, outputDir, "generated", "generated.liveness");

        System.out.println("=== Java classes generated from UML metamodel ===");
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("  - java/generated/*.java");
    }
}



