package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateMilestonePayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneBuilder;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MilestoneTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository("octocat", "hello-world");
    private final GitHubMilestone record = RepositoryContentFixtures.milestone();
    private final Milestone sut = new Milestone(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenMilestoneIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(3);
            softly.assertThat(sut.title()).isEqualTo("v1.0");
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void description_shouldReturnTheDescription_whenTheRecordHasOne() {
        final Milestone described = new Milestone(
                repository,
                GitHubMilestoneBuilder.builder(record)
                        .description("First release")
                        .build());

        final Optional<String> description = described.description();

        assertThat(description).contains("First release");
    }

    @Test
    void close_shouldSendClosedAndReturnTheResponse_whenCalled() throws IOException {
        final GitHubMilestone closed = GitHubMilestoneBuilder.builder(record)
                .state(GitHubMilestoneState.CLOSED)
                .build();
        when(github.issues()
                        .milestones()
                        .updateMilestone(
                                eq("octocat"), eq("hello-world"), eq(3), any(GitHubUpdateMilestonePayload.class)))
                .thenReturn(closed);
        final ArgumentCaptor<GitHubUpdateMilestonePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateMilestonePayload.class);

        final Milestone result = sut.close();

        verify(github.issues().milestones())
                .updateMilestone(eq("octocat"), eq("hello-world"), eq(3), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubMilestoneState.CLOSED);
            softly.assertThat(result.record()).isSameAs(closed);
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void reopen_shouldSendOpenAndReturnTheResponse_whenCalled() throws IOException {
        when(github.issues()
                        .milestones()
                        .updateMilestone(
                                eq("octocat"), eq("hello-world"), eq(3), any(GitHubUpdateMilestonePayload.class)))
                .thenReturn(record);
        final ArgumentCaptor<GitHubUpdateMilestonePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateMilestonePayload.class);

        final Milestone result = sut.reopen();

        verify(github.issues().milestones())
                .updateMilestone(eq("octocat"), eq("hello-world"), eq(3), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubMilestoneState.OPEN);
            softly.assertThat(result.record()).isSameAs(record);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedMilestone_whenGitHubHasIt() throws IOException {
        final GitHubMilestone reloaded =
                GitHubMilestoneBuilder.builder(record).title("v1.1").build();
        when(github.issues().milestones().getMilestone("octocat", "hello-world", 3))
                .thenReturn(Optional.of(reloaded));

        final Milestone refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
            softly.assertThat(sut.title()).isEqualTo("v1.0");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheMilestoneIsGone() throws IOException {
        when(github.issues().milestones().getMilestone("octocat", "hello-world", 3))
                .thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("v1.0");
    }

    @Test
    void delete_shouldDeleteTheMilestone_whenCalled() throws IOException {
        sut.delete();

        verify(github.issues().milestones()).deleteMilestone("octocat", "hello-world", 3);
    }
}
