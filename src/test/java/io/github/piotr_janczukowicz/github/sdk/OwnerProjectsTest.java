package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProject;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.github.sdk.projects.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class OwnerProjectsTest {

    private static final long NUMBER = ProjectFixtures.NUMBER;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void projects_shouldListTheProjectsOfTheOrganization_whenTheyExist() throws IOException {
        final GitHubProject listed = ProjectFixtures.project();
        when(github.projects().project().listForOrg("acme").stream()).thenReturn(Stream.of(listed));
        final Organization sut = organization();

        final List<Project> projects = sut.projects().toList();

        assertThat(projects)
                .singleElement()
                .satisfies(project -> assertThat(project.response()).isSameAs(listed));
    }

    @Test
    void project_shouldLoadTheProjectOfTheOrganization_whenItExists() throws IOException {
        final GitHubProject loaded = ProjectFixtures.project();
        when(github.projects().project().getForOrg("acme", NUMBER)).thenReturn(Optional.of(loaded));
        final Organization sut = organization();

        final Optional<Project> found = sut.project(NUMBER);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void projects_shouldListTheProjectsOfTheUser_whenTheyExist() throws IOException {
        final GitHubProject listed = ProjectFixtures.project();
        when(github.projects().project().listForUser("octocat").stream()).thenReturn(Stream.of(listed));
        final User sut = user();

        final List<Project> projects = sut.projects().toList();

        assertThat(projects)
                .singleElement()
                .satisfies(project -> assertThat(project.response()).isSameAs(listed));
    }

    @Test
    void project_shouldLoadTheProjectOfTheUser_whenItExists() throws IOException {
        final GitHubProject loaded = ProjectFixtures.project();
        when(github.projects().project().getForUser("octocat", NUMBER)).thenReturn(Optional.of(loaded));
        final User sut = user();

        final Optional<Project> found = sut.project(NUMBER);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        return new Organization(sdk, response);
    }

    private User user() {
        final GitHubUser response = mock(GitHubUser.class);
        lenient().when(response.login()).thenReturn("octocat");
        return new User(sdk, response);
    }
}
