package datalogllm.uml.manual;


import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;
import datalogllm.pipeline.umlMetamodel.Association;
import datalogllm.pipeline.umlMetamodel.AssociationEnd;
import datalogllm.pipeline.umlMetamodel.UmlAttribute;
import datalogllm.pipeline.umlMetamodel.UmlClass;
import datalogllm.pipeline.umlMetamodel.UmlModel;

/**
 * Simple test runner for PlantUmlParser using the UML metamodel example.
 * Run with: mvn -q test-compile && java -cp target/test-classes;target/classes PlantUmlParserTest
 * Or run the main method from your IDE.
 */
public final class PlantUmlParserTest {

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
            AssociationEnd "0..*" -- "1" Class : "apunta a"
            @enduml
            """;

    public static void main(String[] args) {
        UmlModel model = PlantUmlParser.parse(EXAMPLE);

        System.out.println("=== PlantUmlParser Test (UML metamodel example) ===\n");

        System.out.println("Classes (" + model.getClasses().size() + "):");
        for (UmlClass c : model.getClasses()) {
            System.out.println("  - " + c.getClassName());
            for (UmlAttribute a : c.getAttributes()) {
                System.out.println("      + " + a.getName() + " : " + a.getType());
            }
        }

        System.out.println("\nAssociations (" + model.getAssociations().size() + "):");
        for (Association a : model.getAssociations()) {
            System.out.println("  - " + a.getId() + " name=" + a.getName() + " (ends=" + a.getEnds().size() + ")");
        }

        System.out.println("\nAssociationEnds (" + model.getAssociationEnds().size() + "):");
        for (AssociationEnd end : model.getAssociationEnds()) {
            String maxStr = end.getMax() == -1 ? "*" : String.valueOf(end.getMax());
            System.out.println("  - assoc=" + end.getAssociation().getId()
                    + " class=" + end.getType().getClassName()
                    + " role=" + end.getRoleName()
                    + " " + end.getMin() + ".." + maxStr
                    + " navigable=" + end.isNavigable());
        }

        System.out.println("\n=== Test OK ===");
    }
}



