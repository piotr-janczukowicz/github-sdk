package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.ratelimit.GitHubRateLimitOverview;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class GitHubSdkTest {

    @Test
    void of_shouldThrowNullPointerException_whenGithubIsNull() {
        final Throwable thrown = catchThrowable(() -> GitHubSdk.of(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("github");
    }

    @Test
    void repository_shouldReturnHandleWithoutRequest_whenCoordinatesAreValid() {
        final GitHub github = mock(GitHub.class);
        final GitHubSdk sut = GitHubSdk.of(github);

        final Repository repository = sut.repository("octocat", "hello-world");

        assertSoftly(softly -> {
            softly.assertThat(repository.owner()).isEqualTo("octocat");
            softly.assertThat(repository.name()).isEqualTo("hello-world");
        });
        verifyNoInteractions(github);
    }

    @Test
    void repository_shouldThrowIllegalArgumentException_whenOwnerIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class));

        final Throwable thrown = catchThrowable(() -> sut.repository(" ", "hello-world"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("owner");
    }

    @Test
    void repository_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class));

        final Throwable thrown = catchThrowable(() -> sut.repository("octocat", ""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void currentUser_shouldReturnTheAuthenticatedUser_whenCalled() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        final GitHubUserView record = mock(GitHubUser.class);
        final GitHubUserApi userApi = github.users().user();
        doReturn(record).when(userApi).getAuthenticatedUser();
        final GitHubSdk sut = GitHubSdk.of(github);

        final User user = sut.currentUser();

        assertThat(user.record()).isSameAs(record);
    }

    @Test
    void user_shouldReturnTheUser_whenGitHubHasTheLogin() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        final GitHubUserView record = mock(GitHubUser.class);
        when(github.users().user().getByLogin("octocat")).thenReturn(Optional.of(record));
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<User> user = sut.user("octocat");

        assertThat(user).map(User::record).containsSame(record);
    }

    @Test
    void user_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        when(github.users().user().getByLogin("ghost")).thenReturn(Optional.empty());
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<User> user = sut.user("ghost");

        assertThat(user).isEmpty();
    }

    @Test
    void user_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class, RETURNS_DEEP_STUBS));

        final Throwable thrown = catchThrowable(() -> sut.user(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void organization_shouldReturnTheOrganization_whenGitHubHasTheLogin() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        final GitHubOrganizationFull record = mock(GitHubOrganizationFull.class);
        when(github.organizations().organization().get("octo-org")).thenReturn(Optional.of(record));
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<Organization> organization = sut.organization("octo-org");

        assertThat(organization).map(Organization::record).containsSame(record);
    }

    @Test
    void organization_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        when(github.organizations().organization().get("ghost-org")).thenReturn(Optional.empty());
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<Organization> organization = sut.organization("ghost-org");

        assertThat(organization).isEmpty();
    }

    @Test
    void organization_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class, RETURNS_DEEP_STUBS));

        final Throwable thrown = catchThrowable(() -> sut.organization(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void search_shouldReturnTheSearchOfTheSdk_whenCalled() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class));

        final Search search = sut.search();

        assertThat(search).isNotNull();
    }

    @Test
    void rateLimit_shouldReturnTheRateLimit_whenGitHubReportsIt() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        final GitHubRateLimitOverview record = mock(GitHubRateLimitOverview.class);
        when(github.rateLimit().get()).thenReturn(Optional.of(record));
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<RateLimit> rateLimit = sut.rateLimit();

        assertThat(rateLimit).map(RateLimit::record).containsSame(record);
    }

    @Test
    void rateLimit_shouldReturnEmpty_whenGitHubDoesNotReportIt() throws IOException {
        final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
        when(github.rateLimit().get()).thenReturn(Optional.empty());
        final GitHubSdk sut = GitHubSdk.of(github);

        final Optional<RateLimit> rateLimit = sut.rateLimit();

        assertThat(rateLimit).isEmpty();
    }
}
