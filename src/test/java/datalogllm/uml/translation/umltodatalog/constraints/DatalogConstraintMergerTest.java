package datalogllm.uml.translation.umltodatalog.constraints;

import datalogllm.pipeline.translation.umltodatalog.constraints.DatalogConstraintMerger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatalogConstraintMergerTest {

    @Test
    void merge_renumbersLlmConstraintIdsAfterStructuralOnes() {
        String structural = """
                % header
                @1 :- A(X), not(B(X))
                @2 :- C(X), X<=0
                """;

        String llm = """
                @1 :- A(X), X>10
                @2 :- C(X), X<100
                """;

        String merged = DatalogConstraintMerger.merge(structural, llm);
        assertThat(merged).contains("@1 :- A(X), not(B(X))");
        assertThat(merged).contains("@2 :- C(X), X<=0");
        assertThat(merged).contains("@3 :- A(X), X>10");
        assertThat(merged).contains("@4 :- C(X), X<100");
    }

    @Test
    void merge_blankLlmConstraints_returnsStructuralUnchanged() {
        String structural = "@1 :- Person(P0, P1)";

        assertThat(DatalogConstraintMerger.merge(structural, "")).isEqualTo(structural);
        assertThat(DatalogConstraintMerger.merge(structural, "   ")).isEqualTo(structural);
    }

    @Test
    void merge_nullLlmConstraints_returnsStructuralUnchanged() {
        String structural = "@1 :- Person(P0, P1)";

        assertThat(DatalogConstraintMerger.merge(structural, null)).isEqualTo(structural);
    }

    @Test
    void merge_appendsLlmSectionHeader() {
        String merged = DatalogConstraintMerger.merge("@1 :- A(X)", "@1 :- B(X)");

        assertThat(merged).contains("%+ OCL constraints (LLM)");
        assertThat(merged).contains("@2 :- B(X)");
    }

    @Test
    void merge_whenStructuralHasNoIds_startsLlmAtOne() {
        String merged = DatalogConstraintMerger.merge("Person(P0, P1).", "@1 :- A(X)");

        assertThat(merged).contains("@1 :- A(X)");
    }
}
