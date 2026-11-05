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
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubListForRepoFilter;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayloadBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubListPullRequestsFilter;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestSimple;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PullRequestsFixtures;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void fetch_shouldReturnLoadedRepository_whenGitHubHasIt() throws IOException {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("Octocat");
        final GitHubFullRepository response = mock(GitHubFullRepository.class);
        when(response.owner()).thenReturn(owner);
        when(response.name()).thenReturn("Hello-World");
        when(github.repositories().repository().get(OWNER, NAME)).thenReturn(Optional.of(response));

        final Optional<Repository> loaded = sut.fetch();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(loaded.orElseThrow().owner()).isEqualTo("Octocat");
            softly.assertThat(loaded.orElseThrow().name()).isEqualTo("Hello-World");
            softly.assertThat(loaded.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void fetch_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.repositories().repository().get(OWNER, NAME)).thenReturn(Optional.empty());

        final Optional<Repository> loaded = sut.fetch();

        assertThat(loaded).isEmpty();
    }

    @Test
    void response_shouldThrowIllegalStateException_whenRepositoryIsAHandle() {
        final Throwable thrown = catchThrowable(sut::response);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("octocat/hello-world");
    }

    @Test
    void issue_shouldReturnIssue_whenGitHubHasIt() throws IOException {
        final GitHubIssue response = IssuesFixtures.issue();
        when(github.issues().issue().get(OWNER, NAME, 42)).thenReturn(Optional.of(response));

        final Optional<Issue> issue = sut.issue(42);

        assertSoftly(softly -> {
            softly.assertThat(issue).isPresent();
            softly.assertThat(issue.orElseThrow().response()).isSameAs(response);
            softly.assertThat(issue.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void issue_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, 42)).thenReturn(Optional.empty());

        final Optional<Issue> issue = sut.issue(42);

        assertThat(issue).isEmpty();
    }

    @Test
    void issues_shouldWrapEachIssue_whenCalledWithAFilter() throws IOException {
        final GitHubIssue response = IssuesFixtures.issue();
        when(github
                        .issues()
                        .issue()
                        .listForRepo(
                                eq(OWNER), eq(NAME), any(GitHubListForRepoFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(response));

        final List<Issue> issues = sut.issues(GitHubListForRepoFilter.empty()).toList();

        assertSoftly(softly -> {
            softly.assertThat(issues).hasSize(1);
            softly.assertThat(issues.getFirst().response()).isSameAs(response);
            softly.assertThat(issues.getFirst().repository()).isSameAs(sut);
        });
    }

    @Test
    void issues_shouldListWithAnEmptyFilter_whenCalledWithoutOne() throws IOException {
        when(github
                        .issues()
                        .issue()
                        .listForRepo(
                                eq(OWNER), eq(NAME), any(GitHubListForRepoFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.empty());

        final List<Issue> issues = sut.issues().toList();

        assertThat(issues).isEmpty();
    }

    @Test
    void issues_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.issues(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void createIssue_shouldSendTheTitle_whenCalledWithATitle() throws IOException {
        final GitHubIssue response = IssuesFixtures.issue();
        when(github.issues().issue().create(eq(OWNER), eq(NAME), any(GitHubCreatePayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubCreatePayload> payload = ArgumentCaptor.forClass(GitHubCreatePayload.class);

        final Issue issue = sut.createIssue("Found a bug");

        verify(github.issues().issue()).create(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("Found a bug");
            softly.assertThat(issue.response()).isSameAs(response);
            softly.assertThat(issue.repository()).isSameAs(sut);
        });
    }

    @Test
    void createIssue_shouldSendThePayload_whenCalledWithAPayload() throws IOException {
        final GitHubIssue response = IssuesFixtures.issue();
        final GitHubCreatePayload payload = GitHubCreatePayloadBuilder.builder()
                .title("Found a bug")
                .body("Details")
                .build();
        when(github.issues().issue().create(OWNER, NAME, payload)).thenReturn(response);

        final Issue issue = sut.createIssue(payload);

        assertThat(issue.response()).isSameAs(response);
    }

    @Test
    void createIssue_shouldThrowNullPointerException_whenTitleIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createIssue((String) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("title");
    }

    @Test
    void createIssue_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createIssue((GitHubCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void pullRequest_shouldReturnPullRequest_whenGitHubHasIt() throws IOException {
        final GitHubPullRequest response = PullRequestsFixtures.pullRequest();
        when(github.pullRequests().pullRequest().get(OWNER, NAME, 7)).thenReturn(Optional.of(response));

        final Optional<PullRequest> pullRequest = sut.pullRequest(7);

        assertSoftly(softly -> {
            softly.assertThat(pullRequest).isPresent();
            softly.assertThat(pullRequest.orElseThrow().response()).isSameAs(response);
            softly.assertThat(pullRequest.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void pullRequest_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.pullRequests().pullRequest().get(OWNER, NAME, 7)).thenReturn(Optional.empty());

        final Optional<PullRequest> pullRequest = sut.pullRequest(7);

        assertThat(pullRequest).isEmpty();
    }

    @Test
    void pullRequests_shouldLoadEachListedPullRequest_whenCalledWithAFilter() throws IOException {
        final GitHubPullRequestSimple listed = mock(GitHubPullRequestSimple.class);
        when(listed.number()).thenReturn(7);
        final GitHubPullRequest response = PullRequestsFixtures.pullRequest();
        when(github
                        .pullRequests()
                        .pullRequest()
                        .list(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListPullRequestsFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.pullRequests().pullRequest().get(OWNER, NAME, 7)).thenReturn(Optional.of(response));

        final List<PullRequest> pullRequests =
                sut.pullRequests(GitHubListPullRequestsFilter.empty()).toList();

        assertThat(pullRequests).extracting(PullRequest::response).containsExactly(response);
    }

    @Test
    void pullRequests_shouldListWithAnEmptyFilter_whenCalledWithoutOne() throws IOException {
        when(github
                        .pullRequests()
                        .pullRequest()
                        .list(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListPullRequestsFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.empty());

        final List<PullRequest> pullRequests = sut.pullRequests().toList();

        assertThat(pullRequests).isEmpty();
    }

    @Test
    void pullRequests_shouldSkipTheItem_whenAListedPullRequestIsGone() throws IOException {
        final GitHubPullRequestSimple listed = mock(GitHubPullRequestSimple.class);
        when(listed.number()).thenReturn(7);
        when(github
                        .pullRequests()
                        .pullRequest()
                        .list(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListPullRequestsFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.pullRequests().pullRequest().get(OWNER, NAME, 7)).thenReturn(Optional.empty());
        final Stream<PullRequest> pullRequests = sut.pullRequests();

        final long count = pullRequests.count();

        assertThat(count).isZero();
    }

    @Test
    void pullRequests_shouldThrowUncheckedIoException_whenLoadingAListedPullRequestFails() throws IOException {
        final GitHubPullRequestSimple listed = mock(GitHubPullRequestSimple.class);
        when(listed.number()).thenReturn(7);
        when(github
                        .pullRequests()
                        .pullRequest()
                        .list(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListPullRequestsFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        when(github.pullRequests().pullRequest().get(OWNER, NAME, 7)).thenThrow(new IOException("transport failed"));
        final Stream<PullRequest> pullRequests = sut.pullRequests();

        final Throwable thrown = catchThrowable(pullRequests::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void createPullRequest_shouldSendThePayloadAndReturnThePullRequest_whenCalled() throws IOException {
        final GitHubPullRequest response = PullRequestsFixtures.pullRequest();
        final GitHubCreatePullRequestPayload payload = GitHubCreatePullRequestPayloadBuilder.builder()
                .head("feature")
                .base("main")
                .title("Add feature")
                .build();
        when(github.pullRequests().pullRequest().create(OWNER, NAME, payload)).thenReturn(response);

        final PullRequest pullRequest = sut.createPullRequest(payload);

        assertSoftly(softly -> {
            softly.assertThat(pullRequest.response()).isSameAs(response);
            softly.assertThat(pullRequest.repository()).isSameAs(sut);
        });
    }

    @Test
    void createPullRequest_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createPullRequest(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }
}
