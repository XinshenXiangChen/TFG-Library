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
}
