package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubPullRequest response = PullRequestsFixtures.pullRequest();
    private final PullRequest pullRequest = new PullRequest(GitHubSdk.of(github).repository(OWNER, NAME), response);

    @Test
    void send_shouldSendOnlyTheSetFields_whenTitleAndBaseAreSet() throws IOException {
        final GitHubPullRequest updated =
                GitHubPullRequestBuilder.builder(response).title("New title").build();
        when(github.pullRequests()
                        .pullRequest()
                        .update(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdatePullRequestPayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdatePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdatePullRequestPayload.class);

        final PullRequest result =
                pullRequest.update().title("New title").base("develop").send();

        verify(github.pullRequests().pullRequest()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("New title");
            softly.assertThat(payload.getValue().base()).isEqualTo("develop");
            softly.assertThat(payload.getValue().body()).isNull();
            softly.assertThat(payload.getValue().state()).isNull();
            softly.assertThat(payload.getValue().maintainerCanModify()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(pullRequest.title()).isEqualTo("Add feature");
        });
    }

    @Test
    void send_shouldSendBodyAndMaintainerFlag_whenTheyAreSet() throws IOException {
        when(github.pullRequests()
                        .pullRequest()
                        .update(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdatePullRequestPayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdatePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdatePullRequestPayload.class);

        pullRequest.update().body("Details").maintainerCanModify(false).send();

        verify(github.pullRequests().pullRequest()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Details");
            softly.assertThat(payload.getValue().maintainerCanModify()).isFalse();
        });
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final PullRequestUpdate update = pullRequest.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.pullRequests().pullRequest());
    }

    @Test
    void title_shouldThrowNullPointerException_whenTitleIsNull() {
        final PullRequestUpdate update = pullRequest.update();

        final Throwable thrown = catchThrowable(() -> update.title(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("title");
    }

    @Test
    void body_shouldThrowNullPointerException_whenBodyIsNull() {
        final PullRequestUpdate update = pullRequest.update();

        final Throwable thrown = catchThrowable(() -> update.body(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void base_shouldThrowNullPointerException_whenBaseIsNull() {
        final PullRequestUpdate update = pullRequest.update();

        final Throwable thrown = catchThrowable(() -> update.base(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("base");
    }
}
