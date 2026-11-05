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
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubMergePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeMethod;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReview;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRemoveReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRequestReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubPullRequest response = PullRequestsFixtures.pullRequest();
    private final PullRequest sut = new PullRequest(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenPullRequestIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.title()).isEqualTo("Add feature");
            softly.assertThat(sut.state()).isEqualTo(PullRequestState.OPEN);
            softly.assertThat(sut.author()).isSameAs(response.user());
            softly.assertThat(sut.isDraft()).isFalse();
            softly.assertThat(sut.isMerged()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isDraft_shouldBeTrue_whenTheResponseSaysDraft() {
        final PullRequest draft = new PullRequest(
                repository,
                GitHubPullRequestBuilder.builder(response).draft(true).build());

        final boolean isDraft = draft.isDraft();

        assertThat(isDraft).isTrue();
    }

    @Test
    void isMerged_shouldBeTrue_whenTheResponseSaysMerged() {
        final PullRequest merged = new PullRequest(
                repository,
                GitHubPullRequestBuilder.builder(response).merged(true).build());

        final boolean isMerged = merged.isMerged();

        assertThat(isMerged).isTrue();
    }

    @Test
    void refresh_shouldReturnTheReloadedPullRequest_whenGitHubHasIt() throws IOException {
        final GitHubPullRequest reloaded =
                GitHubPullRequestBuilder.builder(response).title("Renamed").build();
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final PullRequest refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
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
    void close_shouldSendClosedAndReturnTheResponse_whenCalled() throws IOException {
        final GitHubPullRequest closed = GitHubPullRequestBuilder.builder(response)
                .state(GitHubPullRequestUpdateState.CLOSED)
                .build();
        when(github.pullRequests()
                        .pullRequest()
                        .update(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdatePullRequestPayload.class)))
                .thenReturn(closed);
        final ArgumentCaptor<GitHubUpdatePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdatePullRequestPayload.class);

        final PullRequest result = sut.close();

        verify(github.pullRequests().pullRequest()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubPullRequestUpdateState.CLOSED);
            softly.assertThat(result.state()).isEqualTo(PullRequestState.CLOSED);
            softly.assertThat(sut.state()).isEqualTo(PullRequestState.OPEN);
        });
    }

    @Test
    void reopen_shouldSendOpenAndReturnTheResponse_whenCalled() throws IOException {
        when(github.pullRequests()
                        .pullRequest()
                        .update(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdatePullRequestPayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdatePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdatePullRequestPayload.class);

        final PullRequest result = sut.reopen();

        verify(github.pullRequests().pullRequest()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubPullRequestUpdateState.OPEN);
            softly.assertThat(result.response()).isSameAs(response);
        });
    }

    @Test
    void merge_shouldSendTheMethodAndReturnTheReloadedPullRequest_whenCalledWithAMethod() throws IOException {
        final GitHubPullRequest merged =
                GitHubPullRequestBuilder.builder(response).merged(true).build();
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(merged));
        final ArgumentCaptor<GitHubMergePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubMergePullRequestPayload.class);

        final PullRequest result = sut.merge(MergeMethod.SQUASH);

        verify(github.pullRequests().pullRequest()).merge(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().mergeMethod()).isEqualTo(GitHubPullRequestMergeMethod.SQUASH);
            softly.assertThat(payload.getValue().commitMessage()).isNull();
            softly.assertThat(result.isMerged()).isTrue();
            softly.assertThat(sut.isMerged()).isFalse();
        });
    }

    @Test
    void merge_shouldSendTheCommitMessage_whenCalledWithAMessage() throws IOException {
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubMergePullRequestPayload> payload =
                ArgumentCaptor.forClass(GitHubMergePullRequestPayload.class);

        sut.merge(MergeMethod.MERGE, "Merge the feature");

        verify(github.pullRequests().pullRequest()).merge(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().mergeMethod()).isEqualTo(GitHubPullRequestMergeMethod.MERGE);
            softly.assertThat(payload.getValue().commitMessage()).isEqualTo("Merge the feature");
        });
    }

    @Test
    void merge_shouldThrowNullPointerException_whenMethodIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.merge(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("method");
    }

    @Test
    void reviews_shouldStreamTheReviews_whenTheyExist() throws IOException {
        final GitHubPullRequestReview review = mock(GitHubPullRequestReview.class);
        when(github.pullRequests().reviews().list(OWNER, NAME, NUMBER).stream()).thenReturn(Stream.of(review));

        final List<GitHubPullRequestReview> reviews = sut.reviews().toList();

        assertThat(reviews).containsExactly(review);
    }

    @Test
    void review_shouldSendThePayloadAndReturnTheReview_whenCalled() throws IOException {
        final GitHubPullRequestReview created = mock(GitHubPullRequestReview.class);
        final GitHubCreateReviewPayload payload = mock(GitHubCreateReviewPayload.class);
        when(github.pullRequests().reviews().create(OWNER, NAME, NUMBER, payload))
                .thenReturn(created);

        final GitHubPullRequestReview review = sut.review(payload);

        assertThat(review).isSameAs(created);
    }

    @Test
    void requestReviewers_shouldSendTheLoginsAndReturnTheReloadedPullRequest_whenCalled() throws IOException {
        final GitHubPullRequest reloaded =
                GitHubPullRequestBuilder.builder(response).title("Reviewed").build();
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubRequestReviewersPayload> payload =
                ArgumentCaptor.forClass(GitHubRequestReviewersPayload.class);

        final PullRequest result = sut.requestReviewers("hubot", "octocat");

        verify(github.pullRequests().reviewRequests()).request(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().reviewers()).containsExactly("hubot", "octocat");
            softly.assertThat(payload.getValue().teamReviewers()).isNullOrEmpty();
            softly.assertThat(result.response()).isSameAs(reloaded);
        });
    }

    @Test
    void requestReviewers_shouldThrowIllegalArgumentException_whenNoLoginIsGiven() {
        final Throwable thrown = catchThrowable(sut::requestReviewers);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("logins");
    }

    @Test
    void removeReviewers_shouldSendTheLoginsAndReturnTheReloadedPullRequest_whenCalled() throws IOException {
        when(github.pullRequests().pullRequest().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubRemoveReviewersPayload> payload =
                ArgumentCaptor.forClass(GitHubRemoveReviewersPayload.class);

        final PullRequest result = sut.removeReviewers("hubot");

        verify(github.pullRequests().reviewRequests()).remove(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().reviewers()).containsExactly("hubot");
            softly.assertThat(result.response()).isSameAs(response);
        });
    }

    @Test
    void removeReviewers_shouldThrowIllegalArgumentException_whenNoLoginIsGiven() {
        final Throwable thrown = catchThrowable(sut::removeReviewers);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("logins");
    }

    @Test
    void issue_shouldLoadTheIssueWithTheSameNumber_whenItExists() throws IOException {
        final GitHubIssue issueResponse = IssuesFixtures.issue();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(issueResponse));

        final Issue issue = sut.issue();

        assertSoftly(softly -> {
            softly.assertThat(issue.response()).isSameAs(issueResponse);
            softly.assertThat(issue.repository()).isSameAs(repository);
        });
    }

    @Test
    void issue_shouldThrowIllegalStateException_whenTheIssueIsGone() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::issue);

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
