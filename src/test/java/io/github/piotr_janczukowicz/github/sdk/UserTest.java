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
        final GitHubUser record = mock(GitHubUser.class);
        when(record.login()).thenReturn("octocat");
        final User sut = new User(sdk, record);

        final String login = sut.login();

        assertThat(login).isEqualTo("octocat");
    }

    @Test
    void login_shouldReadTheLogin_whenTheViewIsTheAuthenticatedUser() {
        final GitHubAuthenticatedUser record = mock(GitHubAuthenticatedUser.class);
        when(record.login()).thenReturn("hubot");
        final User sut = new User(sdk, record);

        final String login = sut.login();

        assertThat(login).isEqualTo("hubot");
    }

    @Test
    void name_shouldReturnTheName_whenTheUserHasOne() {
        final GitHubUser record = mock(GitHubUser.class);
        when(record.name()).thenReturn("The Octocat");
        final User sut = new User(sdk, record);

        final Optional<String> name = sut.name();

        assertThat(name).contains("The Octocat");
    }

    @Test
    void name_shouldReturnTheNameOfTheAuthenticatedUser_whenItHasOne() {
        final GitHubAuthenticatedUser record = mock(GitHubAuthenticatedUser.class);
        when(record.name()).thenReturn("Hubot");
        final User sut = new User(sdk, record);

        final Optional<String> name = sut.name();

        assertThat(name).contains("Hubot");
    }

    @Test
    void name_shouldBeEmpty_whenTheUserHasNone() {
        final GitHubUser record = mock(GitHubUser.class);
        when(record.name()).thenReturn(null);
        final User sut = new User(sdk, record);

        final Optional<String> name = sut.name();

        assertThat(name).isEmpty();
    }

    @Test
    void record_shouldReturnTheView_whenCalled() {
        final GitHubUserView record = mock(GitHubUser.class);
        final User sut = new User(sdk, record);

        final GitHubUserView view = sut.record();

        assertThat(view).isSameAs(record);
    }

    @Test
    void repositories_shouldReturnHandlesOfTheListedRepositories_whenTheUserHasRepositories() throws IOException {
        final GitHubUser record = mock(GitHubUser.class);
        when(record.login()).thenReturn("octocat");
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("octocat");
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("hello-world");
        when(github.repositories().repository().listForUser("octocat").stream()).thenReturn(Stream.of(listed));
        final User sut = new User(sdk, record);

        final List<Repository> repositories = sut.repositories().toList();

        assertThat(repositories).hasSize(1);
        assertSoftly(softly -> {
            softly.assertThat(repositories.getFirst().owner()).isEqualTo("octocat");
            softly.assertThat(repositories.getFirst().name()).isEqualTo("hello-world");
        });
    }

    private User userWithOrganizations(final GitHubOrganizationSimple... listed) throws IOException {
        final GitHubUser record = mock(GitHubUser.class);
        when(record.login()).thenReturn("octocat");
        when(github.organizations().organization().listForUser("octocat").stream())
                .thenReturn(Stream.of(listed));
        return new User(sdk, record);
    }

    private GitHubOrganizationSimple listedOrganization(final String login) {
        final GitHubOrganizationSimple listed = mock(GitHubOrganizationSimple.class);
        when(listed.login()).thenReturn(login);
        return listed;
    }

    @Test
    void organizations_shouldLoadEachListedOrganization_whenTheUserBelongsToSome() throws IOException {
        final GitHubOrganizationFull organizationRecord = mock(GitHubOrganizationFull.class);
        when(github.organizations().organization().get("octo-org")).thenReturn(Optional.of(organizationRecord));
        final User sut = userWithOrganizations(listedOrganization("octo-org"));

        final List<Organization> organizations = sut.organizations().toList();

        assertThat(organizations).extracting(Organization::record).containsExactly(organizationRecord);
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
