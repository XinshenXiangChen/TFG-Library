package datalogllm.uml.pipeline.patients;

import datalogllm.pipeline.PlantUmlPipeline;
import datalogllm.pipeline.translation.umltodatalog.constraints.NaturalLanguageConstraintGenerator;
import edu.upc.fib.inlab.imp.kse.logics.logicschema.services.parser.LogicSchemaWithIDsParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * LLM branch tested with a deterministic stub generator (no Gemini API key required).
 */
class PatientsWithStubLlmConstraintsTest {

    private static final NaturalLanguageConstraintGenerator STUB_GENERATOR = (structural, nl) -> """
            %+ OCL constraints (LLM) — stub
            @99 :- Appointment(A0, A1), A0<=0
            """;

    @Test
    void stubLlm_mergeIsParseable_andAppendsRenumberedConstraints(@TempDir Path outputDir) throws IOException {
        PlantUmlPipeline.generateDatalogAndJsonWithLlmConstraints(
                PatientsDiagramFixtures.WITH_NL_CONSTRAINTS,
                outputDir,
                STUB_GENERATOR);

        Path datalogPath = outputDir.resolve("schema.dl");
        assertThat(datalogPath).exists();
        String datalog = Files.readString(datalogPath, StandardCharsets.UTF_8);

        assertThat(datalog)
                .contains("%+ OCL constraints (LLM)")
                .contains("Appointment(")
                .doesNotContain("@99 :-");

        assertThatCode(() -> new LogicSchemaWithIDsParser().parse(datalog))
                .doesNotThrowAnyException();
    }
}
