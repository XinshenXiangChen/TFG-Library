package datalogllm.pipeline;

import datalogllm.pipeline.translation.umltodatalog.constraints.ConstraintGeneratorFactory;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import datalogllm.pipeline.translation.umltodatalog.parser.UmlParser;
import datalogllm.pipeline.umlMetamodel.services.datalog.UmlToDatalogTranslationService;
import datalogllm.uml.pipeline.patients.PatientsDiagramFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that UML parsing and NL generation are wired as interchangeable strategies.
 */
class StrategyPatternWiringTest {

    private static final NaturalLanguageConstraintGenerator STUB_NL = (structural, nl) -> """
            %+ OCL constraints (LLM) — stub
            @99 :- Appointment(A0, A1), A0<=0
            """;

    @Test
    void customUmlParserStrategy_isUsedByDatalogService(@TempDir Path outputDir) throws Exception {
        UmlParser countingParser = umlText -> {
            UmlParser delegate = PipelineConfig.structuralDefaults().umlParser();
            return delegate.parse(umlText);
        };

        UmlToDatalogTranslationService service = new UmlToDatalogTranslationService(
                countingParser,
                ConstraintGeneratorFactory.noOp());

        service.translate(PatientsDiagramFixtures.STRUCTURAL_PLANTUML, outputDir);

        assertThat(outputDir.resolve("schema.dl")).exists();
        String datalog = Files.readString(outputDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        assertThat(datalog).contains("Person(").contains("Appointment(");
    }

    @Test
    void customNlGeneratorStrategy_mergesIntoSchema(@TempDir Path outputDir) throws Exception {
        PipelineConfig config = PipelineConfig.structuralDefaults()
                .withNlConstraintGenerator(STUB_NL);

        UmlToDatalogTranslationService service = new UmlToDatalogTranslationService(config);
        service.translateWithDefaultNlStrategy(PatientsDiagramFixtures.WITH_NL_CONSTRAINTS, outputDir);

        String datalog = Files.readString(outputDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        assertThat(datalog)
                .contains("%+ OCL constraints (LLM)")
                .doesNotContain("@99 :-");
    }

    @Test
    void noOpNlStrategy_leavesStructuralDatalogUnchanged(@TempDir Path outputDir) throws Exception {
        UmlToDatalogTranslationService structural = new UmlToDatalogTranslationService(
                PipelineConfig.structuralDefaults().umlParser(),
                ConstraintGeneratorFactory.noOp());
        UmlToDatalogTranslationService withNoOp = new UmlToDatalogTranslationService(
                PipelineConfig.structuralDefaults());

        Path structuralDir = outputDir.resolve("structural");
        Path noopDir = outputDir.resolve("noop");

        structural.translate(PatientsDiagramFixtures.WITH_NL_CONSTRAINTS, structuralDir);
        withNoOp.translateWithDefaultNlStrategy(PatientsDiagramFixtures.WITH_NL_CONSTRAINTS, noopDir);

        String structuralDl = Files.readString(structuralDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        String noopDl = Files.readString(noopDir.resolve("schema.dl"), StandardCharsets.UTF_8);
        assertThat(noopDl).isEqualTo(structuralDl);
    }
}
