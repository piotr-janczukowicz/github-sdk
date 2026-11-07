package io.github.piotr_janczukowicz.github.sdk.pulls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PullRequestStateTest {

    @Test
    void from_shouldMapOpen_whenTheClientValueIsOpen() {
        final PullRequestState state = PullRequestState.from(GitHubPullRequestUpdateState.OPEN);

        assertThat(state).isEqualTo(PullRequestState.OPEN);
    }

    @Test
    void from_shouldMapClosed_whenTheClientValueIsClosed() {
        final PullRequestState state = PullRequestState.from(GitHubPullRequestUpdateState.CLOSED);

        assertThat(state).isEqualTo(PullRequestState.CLOSED);
    }

    @Test
    void from_shouldThrowNullPointerException_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> PullRequestState.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }
}
