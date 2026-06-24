package datalogllm.pipeline.translation.umltodatalog.constraints;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstraintGeneratorFactoryTest {

    @Test
    void noOp_returnsSingletonInstance() {
        NaturalLanguageConstraintGenerator first = ConstraintGeneratorFactory.noOp();
        NaturalLanguageConstraintGenerator second = ConstraintGeneratorFactory.noOp();

        assertThat(first).isSameAs(second);
        assertThat(first).isInstanceOf(NoOpNaturalLanguageConstraintGenerator.class);
    }

    @Test
    void noOpGenerator_returnsEmptyString() {
        String result = ConstraintGeneratorFactory.noOp().generateConstraints(
                "@1 :- Person(P0, P1)",
                "Some NL constraint");

        assertThat(result).isEmpty();
    }

    @Test
    void fromApiKey_withBlankKey_throws() {
        assertThatThrownBy(() -> ConstraintGeneratorFactory.fromApiKey(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromApiKey_returnsGeminiGenerator() {
        NaturalLanguageConstraintGenerator generator = ConstraintGeneratorFactory.fromApiKey("test-key");

        assertThat(generator).isInstanceOf(GeminiNaturalLanguageConstraintGenerator.class);
    }
}
