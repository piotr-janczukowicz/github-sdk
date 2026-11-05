package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationSimple;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepository;
import io.github.piotr_janczukowicz.github.api.users.GitHubAuthenticatedUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class UserTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void login_shouldReadTheLogin_whenTheViewIsAPlainUser() {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn("octocat");
        final User sut = new User(sdk, response);

        final String login = sut.login();

        assertThat(login).isEqualTo("octocat");
    }

    @Test
    void login_shouldReadTheLogin_whenTheViewIsTheAuthenticatedUser() {
        final GitHubAuthenticatedUser response = mock(GitHubAuthenticatedUser.class);
        when(response.login()).thenReturn("hubot");
        final User sut = new User(sdk, response);

        final String login = sut.login();

        assertThat(login).isEqualTo("hubot");
    }

    @Test
    void name_shouldReturnTheName_whenTheUserHasOne() {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.name()).thenReturn("The Octocat");
        final User sut = new User(sdk, response);

        final Optional<String> name = sut.name();

        assertThat(name).contains("The Octocat");
    }

    @Test
    void name_shouldReturnTheNameOfTheAuthenticatedUser_whenItHasOne() {
        final GitHubAuthenticatedUser response = mock(GitHubAuthenticatedUser.class);
        when(response.name()).thenReturn("Hubot");
        final User sut = new User(sdk, response);

        final Optional<String> name = sut.name();

        assertThat(name).contains("Hubot");
    }

    @Test
    void name_shouldBeEmpty_whenTheUserHasNone() {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.name()).thenReturn(null);
        final User sut = new User(sdk, response);

        final Optional<String> name = sut.name();

        assertThat(name).isEmpty();
    }

    @Test
    void response_shouldReturnTheView_whenCalled() {
        final GitHubUserView response = mock(GitHubUser.class);
        final User sut = new User(sdk, response);

        final GitHubUserView view = sut.response();

        assertThat(view).isSameAs(response);
    }

    @Test
    void repositories_shouldReturnHandlesOfTheListedRepositories_whenTheUserHasRepositories() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn("octocat");
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("octocat");
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("hello-world");
        when(github.repositories().repository().listForUser("octocat").stream()).thenReturn(Stream.of(listed));
        final User sut = new User(sdk, response);

        final List<Repository> repositories = sut.repositories().toList();

        assertThat(repositories).hasSize(1);
        assertSoftly(softly -> {
            softly.assertThat(repositories.getFirst().owner()).isEqualTo("octocat");
            softly.assertThat(repositories.getFirst().name()).isEqualTo("hello-world");
        });
    }

    @Test
    void starredRepositories_shouldReturnHandlesOfTheStarredRepositories_whenTheUserStarredSome() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn("octocat");
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("hubot");
        final GitHubRepository listed = mock(GitHubRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("linguist");
        when(github.activity().starring().listReposStarredByUser("octocat").stream())
                .thenReturn(Stream.of(listed));
        final User sut = new User(sdk, response);

        final List<Repository> repositories = sut.starredRepositories().toList();

        assertThat(repositories).singleElement().satisfies(repository -> {
            assertThat(repository.owner()).isEqualTo("hubot");
            assertThat(repository.name()).isEqualTo("linguist");
        });
    }

    @Test
    void watchedRepositories_shouldReturnHandlesOfTheWatchedRepositories_whenTheUserWatchesSome() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn("octocat");
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("hubot");
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("linguist");
        when(github.activity().watching().listReposWatchedByUser("octocat").stream())
                .thenReturn(Stream.of(listed));
        final User sut = new User(sdk, response);

        final List<Repository> repositories = sut.watchedRepositories().toList();

        assertThat(repositories).singleElement().satisfies(repository -> {
            assertThat(repository.owner()).isEqualTo("hubot");
            assertThat(repository.name()).isEqualTo("linguist");
        });
    }

    private User userWithOrganizations(final GitHubOrganizationSimple... listed) throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn("octocat");
        when(github.organizations().organization().listForUser("octocat").stream())
                .thenReturn(Stream.of(listed));
        return new User(sdk, response);
    }

    private GitHubOrganizationSimple listedOrganization(final String login) {
        final GitHubOrganizationSimple listed = mock(GitHubOrganizationSimple.class);
        when(listed.login()).thenReturn(login);
        return listed;
    }

    @Test
    void organizations_shouldLoadEachListedOrganization_whenTheUserBelongsToSome() throws IOException {
        final GitHubOrganizationFull organizationResponse = mock(GitHubOrganizationFull.class);
        when(github.organizations().organization().get("octo-org")).thenReturn(Optional.of(organizationResponse));
        final User sut = userWithOrganizations(listedOrganization("octo-org"));

        final List<Organization> organizations = sut.organizations().toList();

        assertThat(organizations).extracting(Organization::response).containsExactly(organizationResponse);
    }

    @Test
    void organizations_shouldThrowIllegalStateException_whenAListedOrganizationIsGone() throws IOException {
        when(github.organizations().organization().get("octo-org")).thenReturn(Optional.empty());
        final User sut = userWithOrganizations(listedOrganization("octo-org"));
        final Stream<Organization> organizations = sut.organizations();

        final Throwable thrown = catchThrowable(organizations::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("octo-org");
    }

    @Test
    void organizations_shouldThrowUncheckedIoException_whenLoadingAListedOrganizationFails() throws IOException {
        when(github.organizations().organization().get("octo-org")).thenThrow(new IOException("transport failed"));
        final User sut = userWithOrganizations(listedOrganization("octo-org"));
        final Stream<Organization> organizations = sut.organizations();

        final Throwable thrown = catchThrowable(organizations::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }
}
