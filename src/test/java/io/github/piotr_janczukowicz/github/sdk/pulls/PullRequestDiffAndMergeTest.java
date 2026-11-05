package io.github.piotr_janczukowicz.github.sdk.pulls;

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
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubMergePullRequestAsyncPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBranchUpdateResult;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeAsyncResult;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeMethod;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestBranchPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PullRequestsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestDiffAndMergeTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final PullRequest sut = new PullRequest(repository, PullRequestsFixtures.pullRequest());

    @Test
    void diff_shouldReturnTheDiff_whenGitHubHasIt() throws IOException {
        when(github.pullRequests().pullRequest().getDiff(OWNER, NAME, NUMBER)).thenReturn(Optional.of("diff --git"));

        final Optional<String> diff = sut.diff();

        assertThat(diff).contains("diff --git");
    }

    @Test
    void patch_shouldReturnThePatch_whenGitHubHasIt() throws IOException {
        when(github.pullRequests().pullRequest().getPatch(OWNER, NAME, NUMBER)).thenReturn(Optional.of("From abc"));

        final Optional<String> patch = sut.patch();

        assertThat(patch).contains("From abc");
    }

    @Test
    void updateBranch_shouldSendNoPayload_whenCalledWithoutArguments() throws IOException {
        final GitHubPullRequestBranchUpdateResult result = new GitHubPullRequestBranchUpdateResult("Updating", null);
        when(github.pullRequests().pullRequest().updateBranch(OWNER, NAME, NUMBER, null))
                .thenReturn(result);

        final GitHubPullRequestBranchUpdateResult answer = sut.updateBranch();

        assertThat(answer).isSameAs(result);
    }

    @Test
    void updateBranch_shouldSendTheExpectedHead_whenCalledWithASha() throws IOException {
        final GitHubPullRequestBranchUpdateResult result = new GitHubPullRequestBranchUpdateResult("Updating", null);
        when(github.pullRequests().pullRequest().updateBranch(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(result);
        final ArgumentCaptor<GitHubUpdatePullRequestBranchPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdatePullRequestBranchPayload.class);

        final GitHubPullRequestBranchUpdateResult answer = sut.updateBranch("abc123");

        verify(github.pullRequests().pullRequest()).updateBranch(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(answer).isSameAs(result);
            softly.assertThat(payload.getValue().expectedHeadSha()).isEqualTo("abc123");
        });
    }

    @Test
    void updateBranch_shouldThrowIllegalArgumentException_whenTheShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.updateBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expectedHeadSha");
    }

    @Test
    void mergeAsync_shouldSendTheMethod_whenCalledWithAMethod() throws IOException {
        final GitHubPullRequestMergeAsyncResult result = mock(GitHubPullRequestMergeAsyncResult.class);
        when(github.pullRequests().pullRequest().mergeAsync(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(result);
        final ArgumentCaptor<GitHubMergePullRequestAsyncPayload> payload =
                ArgumentCaptor.forClass(GitHubMergePullRequestAsyncPayload.class);

        final GitHubPullRequestMergeAsyncResult answer = sut.mergeAsync(MergeMethod.SQUASH);

        verify(github.pullRequests().pullRequest()).mergeAsync(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(answer).isSameAs(result);
            softly.assertThat(payload.getValue().mergeMethod()).isEqualTo(GitHubPullRequestMergeMethod.SQUASH);
        });
    }

    @Test
    void mergeAsync_shouldSendThePayload_whenCalledWithOne() throws IOException {
        final GitHubMergePullRequestAsyncPayload payload =
                new GitHubMergePullRequestAsyncPayload(null, null, null, null, null, true);
        final GitHubPullRequestMergeAsyncResult result = mock(GitHubPullRequestMergeAsyncResult.class);
        when(github.pullRequests().pullRequest().mergeAsync(OWNER, NAME, NUMBER, payload))
                .thenReturn(result);

        final GitHubPullRequestMergeAsyncResult answer = sut.mergeAsync(payload);

        assertThat(answer).isSameAs(result);
    }

    @Test
    void mergeAsync_shouldThrowNullPointerException_whenTheMethodIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.mergeAsync((MergeMethod) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("method");
    }

    @Test
    void mergeAsync_shouldThrowNullPointerException_whenThePayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.mergeAsync((GitHubMergePullRequestAsyncPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void mergeAsyncResult_shouldReturnTheResult_whenGitHubHasIt() throws IOException {
        final GitHubPullRequestMergeAsyncResult result = mock(GitHubPullRequestMergeAsyncResult.class);
        when(github.pullRequests().pullRequest().getMergeAsyncResult(OWNER, NAME, NUMBER, "uuid-1"))
                .thenReturn(Optional.of(result));

        final Optional<GitHubPullRequestMergeAsyncResult> answer = sut.mergeAsyncResult("uuid-1");

        assertThat(answer).containsSame(result);
    }

    @Test
    void mergeAsyncResult_shouldThrowIllegalArgumentException_whenTheUuidIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.mergeAsyncResult(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("uuid");
    }
}
