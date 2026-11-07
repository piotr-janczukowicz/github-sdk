package io.github.piotr_janczukowicz.github.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class IssueStateTest {

    @ParameterizedTest
    @CsvSource({"open,OPEN", "closed,CLOSED"})
    void from_shouldMapTheWireValue_whenItIsKnown(final String wire, final IssueState expected) {
        final IssueState state = IssueState.from(wire);

        assertThat(state).isEqualTo(expected);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenTheValueIsUnknown() {
        final Throwable thrown = catchThrowable(() -> IssueState.from("archived"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("archived");
    }

    @Test
    void from_shouldThrowNullPointerException_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> IssueState.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }
}
