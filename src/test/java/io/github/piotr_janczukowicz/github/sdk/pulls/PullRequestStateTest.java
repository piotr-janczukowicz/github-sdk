package io.github.piotr_janczukowicz.github.sdk.pulls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PullRequestStateTest {

    @Test
    void from_shouldMapBothValuesOfTheClient_whenTheyAreKnown() {
        assertSoftly(softly -> {
            softly.assertThat(PullRequestState.from(GitHubPullRequestUpdateState.OPEN))
                    .isEqualTo(PullRequestState.OPEN);
            softly.assertThat(PullRequestState.from(GitHubPullRequestUpdateState.CLOSED))
                    .isEqualTo(PullRequestState.CLOSED);
        });
    }

    @Test
    void from_shouldThrowNullPointerException_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> PullRequestState.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }
}
