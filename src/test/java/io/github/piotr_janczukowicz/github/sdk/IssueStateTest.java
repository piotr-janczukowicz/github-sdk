package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class IssueStateTest {

    @Test
    void from_shouldMapTheWireValues_whenTheyAreKnown() {
        assertSoftly(softly -> {
            softly.assertThat(IssueState.from("open")).isEqualTo(IssueState.OPEN);
            softly.assertThat(IssueState.from("closed")).isEqualTo(IssueState.CLOSED);
        });
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
