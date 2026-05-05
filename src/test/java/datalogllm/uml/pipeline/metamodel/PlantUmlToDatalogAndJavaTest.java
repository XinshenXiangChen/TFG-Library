package datalogllm.uml.pipeline.metamodel;

import datalogllm.pipeline.PlantUmlPipeline;
import datalogllm.pipeline.translation.umltodatalog.parser.PlantUmlParser;
import datalogllm.pipeline.umlMetamodel.UmlModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Translation-focused pipeline test:
 * UML -> Datalog and UML metamodel -> Java/liveliness tests.
 */
public final class PlantUmlToDatalogAndJavaTest {

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
            AssociationEnd "0..*" -- "1" Class : apunta_a
            @enduml
            """;

    @Test
    void umlToDatalogWithNlConstraints_thenUmlMetamodelToJavaAndLiveliness() throws IOException {
        Path outputDir = Path.of("target", "generated");
        String javaPackage = "generated";
        String livelinessTestPackage = "generated.liveness";

        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(EXAMPLE, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(EXAMPLE, outputDir, javaPackage, livelinessTestPackage);

        UmlModel model = PlantUmlParser.parse(EXAMPLE);
        Path livenessRoot = outputDir.resolve("liveness-tests").resolve("generated").resolve("liveness");
        assertThat(livenessRoot).exists().isDirectory();

        List<String> expectedTestClasses = model.getClasses().stream()
                .filter(c -> c.getClassName().length() >= 2 && Character.isUpperCase(c.getClassName().charAt(0)))
                .map(c -> c.getClassName() + "LivelinessTest.java")
                .toList();
        for (String name : expectedTestClasses) {
            Path testFile = livenessRoot.resolve(name);
            assertThat(testFile).as("Liveliness test file: " + name).exists();
            String content = Files.readString(testFile);
            assertThat(content)
                    .contains("class " + name.replace(".java", "") + " {")
                    .contains("isSatisfiable")
                    .contains("SATResult")
                    .contains("Files.readString(Path.of(SCHEMA_PATH)")
                    .contains("schema.dl");
        }
    }

    public static void main(String[] args) {
        Path outputDir = Path.of("target", "generated");
        String javaPackage = "generated";
        String livelinessPackage = "generated.liveness";

        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(EXAMPLE, outputDir);
        PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(EXAMPLE, outputDir, javaPackage, livelinessPackage);

        System.out.println("=== PlantUML -> Datalog (with NL constraints), then UML metamodel -> Java + Liveliness ===");
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("  - schema.dl");
        System.out.println("  - java/" + javaPackage.replace('.', '/') + "/*.java");
        System.out.println("  - liveness-tests/" + livelinessPackage.replace('.', '/') + "/*LivelinessTest.java");
    }
}
