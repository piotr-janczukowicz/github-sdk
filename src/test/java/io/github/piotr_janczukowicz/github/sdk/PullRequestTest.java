package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class PullRequestTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubPullRequest record = PullRequestsFixtures.pullRequest();
    private final PullRequest sut = new PullRequest(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenPullRequestIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.title()).isEqualTo("Add feature");
            softly.assertThat(sut.state()).isEqualTo(PullRequestState.OPEN);
            softly.assertThat(sut.author()).isSameAs(record.user());
            softly.assertThat(sut.isDraft()).isFalse();
            softly.assertThat(sut.isMerged()).isFalse();
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void isDraft_shouldBeTrue_whenTheRecordSaysDraft() {
        final PullRequest draft = new PullRequest(
                repository, GitHubPullRequestBuilder.builder(record).draft(true).build());

        final boolean isDraft = draft.isDraft();

        assertThat(isDraft).isTrue();
    }

    @Test
    void isMerged_shouldBeTrue_whenTheRecordSaysMerged() {
        final PullRequest merged = new PullRequest(
                repository,
                GitHubPullRequestBuilder.builder(record).merged(true).build());

        final boolean isMerged = merged.isMerged();

        assertThat(isMerged).isTrue();
    }

    @Test
    void refresh_shouldReturnTheReloadedPullRequest_whenGitHubHasIt() throws IOException {
        final GitHubPullRequest reloaded =
                GitHubPullRequestBuilder.builder(record).title("Renamed").build();
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final PullRequest refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
            softly.assertThat(sut.title()).isEqualTo("Add feature");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenThePullRequestIsGone() throws IOException {
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void files_shouldStreamTheFilesOfThePullRequest_whenTheyExist() throws IOException {
        final GitHubDiffEntry file = mock(GitHubDiffEntry.class);
        when(github.pullRequests().pullRequest().listFiles(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(file));

        final List<GitHubDiffEntry> files = sut.files().toList();

        assertThat(files).containsExactly(file);
    }

    @Test
    void commits_shouldStreamTheCommitsOfThePullRequest_whenTheyExist() throws IOException {
        final GitHubCommit commit = mock(GitHubCommit.class);
        when(github.pullRequests().pullRequest().listCommits(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(commit));

        final List<GitHubCommit> commits = sut.commits().toList();

        assertThat(commits).containsExactly(commit);
    }
}
