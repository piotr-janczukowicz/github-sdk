package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.search.GitHubCodeSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubCommitSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubRepoSearchResultItem;
import io.github.piotr_janczukowicz.github.api.search.GitHubUserSearchResultItem;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class SearchTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Search sut = GitHubSdk.of(github).search();

    private GitHubIssueSearchResultItem foundIssue(final String repositoryUrl, final int number) {
        final GitHubIssueSearchResultItem item = mock(GitHubIssueSearchResultItem.class);
        when(item.repositoryUrl()).thenReturn(repositoryUrl);
        when(item.number()).thenReturn(number);
        return item;
    }

    @Test
    void issues_shouldLoadEachFoundIssue_whenTheQueryMatches() throws IOException {
        final GitHubIssueSearchResultItem item = foundIssue("https://api.github.com/repos/octocat/hello-world", 42);
        final GitHubIssue response = IssuesFixtures.issue();
        when(github.search().searchIssues("is:open bug").stream()).thenReturn(Stream.of(item));
        when(github.issues().issue().get("octocat", "hello-world", 42)).thenReturn(Optional.of(response));

        final List<Issue> issues = sut.issues("is:open bug").toList();

        assertSoftly(softly -> {
            softly.assertThat(issues).hasSize(1);
            softly.assertThat(issues.getFirst().response()).isSameAs(response);
            softly.assertThat(issues.getFirst().repository().owner()).isEqualTo("octocat");
            softly.assertThat(issues.getFirst().repository().name()).isEqualTo("hello-world");
        });
    }

    @Test
    void issues_shouldReadTheRepositoryFromTheLastTwoSegments_whenTheServerIsGhes() throws IOException {
        final GitHubIssueSearchResultItem item =
                foundIssue("https://ghe.example.com/api/v3/repos/octocat/hello-world/", 7);
        when(github.search().searchIssues("bug").stream()).thenReturn(Stream.of(item));
        when(github.issues().issue().get("octocat", "hello-world", 7)).thenReturn(Optional.of(IssuesFixtures.issue()));

        final List<Issue> issues = sut.issues("bug").toList();

        assertThat(issues).extracting(issue -> issue.repository().name()).containsExactly("hello-world");
    }

    @Test
    void issues_shouldSkipTheItem_whenAFoundIssueIsGone() throws IOException {
        final GitHubIssueSearchResultItem item = foundIssue("https://api.github.com/repos/octocat/hello-world", 42);
        when(github.search().searchIssues("bug").stream()).thenReturn(Stream.of(item));
        when(github.issues().issue().get("octocat", "hello-world", 42)).thenReturn(Optional.empty());
        final Stream<Issue> issues = sut.issues("bug");

        final long count = issues.count();

        assertThat(count).isZero();
    }

    @Test
    void issues_shouldThrowUncheckedIoException_whenLoadingAFoundIssueFails() throws IOException {
        final GitHubIssueSearchResultItem item = foundIssue("https://api.github.com/repos/octocat/hello-world", 42);
        when(github.search().searchIssues("bug").stream()).thenReturn(Stream.of(item));
        when(github.issues().issue().get("octocat", "hello-world", 42)).thenThrow(new IOException("transport failed"));
        final Stream<Issue> issues = sut.issues("bug");

        final Throwable thrown = catchThrowable(issues::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void repositories_shouldReturnHandlesOfTheFoundRepositories_whenTheQueryMatches() throws IOException {
        final GitHubRepoSearchResultItem item = mock(GitHubRepoSearchResultItem.class);
        when(item.fullName()).thenReturn("octocat/hello-world");
        when(github.search().searchRepos("hello").stream()).thenReturn(Stream.of(item));

        final List<Repository> repositories = sut.repositories("hello").toList();

        assertSoftly(softly -> {
            softly.assertThat(repositories).hasSize(1);
            softly.assertThat(repositories.getFirst().owner()).isEqualTo("octocat");
            softly.assertThat(repositories.getFirst().name()).isEqualTo("hello-world");
        });
    }

    @Test
    void issues_shouldThrowIllegalArgumentException_whenTheRepositoryUrlHasNoOwner() throws IOException {
        final GitHubIssueSearchResultItem item = mock(GitHubIssueSearchResultItem.class);
        when(item.repositoryUrl()).thenReturn("octocat");
        when(github.search().searchIssues("bug").stream()).thenReturn(Stream.of(item));
        final Stream<Issue> issues = sut.issues("bug");

        final Throwable thrown = catchThrowable(issues::toList);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("octocat");
    }

    @Test
    void repositories_shouldThrowIllegalArgumentException_whenTheFullNameHasNoSlash() throws IOException {
        final GitHubRepoSearchResultItem item = mock(GitHubRepoSearchResultItem.class);
        when(item.fullName()).thenReturn("hello");
        when(github.search().searchRepos("hello").stream()).thenReturn(Stream.of(item));
        final Stream<Repository> repositories = sut.repositories("hello");

        final Throwable thrown = catchThrowable(repositories::toList);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hello");
    }

    @Test
    void repositories_shouldThrowIllegalArgumentException_whenTheFullNameHasNoOwner() throws IOException {
        final GitHubRepoSearchResultItem item = mock(GitHubRepoSearchResultItem.class);
        when(item.fullName()).thenReturn("/hello");
        when(github.search().searchRepos("hello").stream()).thenReturn(Stream.of(item));
        final Stream<Repository> repositories = sut.repositories("hello");

        final Throwable thrown = catchThrowable(repositories::toList);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hello");
    }

    @Test
    void users_shouldLoadEachFoundUser_whenTheQueryMatches() throws IOException {
        final GitHubUserSearchResultItem item = mock(GitHubUserSearchResultItem.class);
        when(item.login()).thenReturn("octocat");
        final GitHubUserView response = mock(GitHubUser.class);
        when(github.search().searchUsers("octo").stream()).thenReturn(Stream.of(item));
        final GitHubUserApi userApi = github.users().user();
        doReturn(Optional.of(response)).when(userApi).getByLogin("octocat");

        final List<User> users = sut.users("octo").toList();

        assertThat(users).extracting(User::response).containsExactly(response);
    }

    @Test
    void users_shouldSkipTheItem_whenAFoundUserIsGone() throws IOException {
        final GitHubUserSearchResultItem item = mock(GitHubUserSearchResultItem.class);
        when(item.login()).thenReturn("octocat");
        when(github.search().searchUsers("octo").stream()).thenReturn(Stream.of(item));
        when(github.users().user().getByLogin("octocat")).thenReturn(Optional.empty());
        final Stream<User> users = sut.users("octo");

        final long count = users.count();

        assertThat(count).isZero();
    }

    @Test
    void users_shouldThrowUncheckedIoException_whenLoadingAFoundUserFails() throws IOException {
        final GitHubUserSearchResultItem item = mock(GitHubUserSearchResultItem.class);
        when(item.login()).thenReturn("octocat");
        when(github.search().searchUsers("octo").stream()).thenReturn(Stream.of(item));
        when(github.users().user().getByLogin("octocat")).thenThrow(new IOException("transport failed"));
        final Stream<User> users = sut.users("octo");

        final Throwable thrown = catchThrowable(users::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void code_shouldStreamTheFoundItems_whenTheQueryMatches() throws IOException {
        final GitHubCodeSearchResultItem item = mock(GitHubCodeSearchResultItem.class);
        when(github.search().searchCode("addClass").stream()).thenReturn(Stream.of(item));

        final List<GitHubCodeSearchResultItem> found = sut.code("addClass").toList();

        assertThat(found).containsExactly(item);
    }

    @Test
    void commits_shouldStreamTheFoundItems_whenTheQueryMatches() throws IOException {
        final GitHubCommitSearchResultItem item = mock(GitHubCommitSearchResultItem.class);
        when(github.search().searchCommits("fix").stream()).thenReturn(Stream.of(item));

        final List<GitHubCommitSearchResultItem> found = sut.commits("fix").toList();

        assertThat(found).containsExactly(item);
    }

    @Test
    void issues_shouldThrowIllegalArgumentException_whenTheQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.issues(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }

    @Test
    void repositories_shouldThrowIllegalArgumentException_whenTheQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.repositories(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }

    @Test
    void users_shouldThrowIllegalArgumentException_whenTheQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.users(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }

    @Test
    void code_shouldThrowIllegalArgumentException_whenTheQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.code(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }

    @Test
    void commits_shouldThrowIllegalArgumentException_whenTheQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.commits(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }
}
