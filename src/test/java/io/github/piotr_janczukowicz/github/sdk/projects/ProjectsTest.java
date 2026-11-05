package io.github.piotr_janczukowicz.github.sdk.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProject;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectsTest {

    private static final long NUMBER = ProjectFixtures.NUMBER;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void all_shouldWrapTheProjectsOfTheOrganization_whenTheyExist() throws IOException {
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects().project().listForOrg("acme").stream()).thenReturn(Stream.of(project));

        final List<Project> projects =
                Projects.forOrganization(sdk, "acme").all().toList();

        assertThat(projects)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(project));
    }

    @Test
    void find_shouldWrapTheProjectOfTheOrganization_whenItExists() throws IOException {
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects().project().getForOrg("acme", NUMBER)).thenReturn(Optional.of(project));

        final Optional<Project> found = Projects.forOrganization(sdk, "acme").find(NUMBER);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(project);
    }

    @Test
    void all_shouldWrapTheProjectsOfTheUser_whenTheyExist() throws IOException {
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects().project().listForUser("octocat").stream()).thenReturn(Stream.of(project));

        final List<Project> projects = Projects.forUser(sdk, "octocat").all().toList();

        assertThat(projects)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(project));
    }

    @Test
    void find_shouldBeEmpty_whenTheUserHasNoSuchProject() throws IOException {
        when(github.projects().project().getForUser("octocat", NUMBER)).thenReturn(Optional.empty());

        final Optional<Project> found = Projects.forUser(sdk, "octocat").find(NUMBER);

        assertThat(found).isEmpty();
    }

    @Test
    void forOrganization_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> Projects.forOrganization(sdk, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void forOrganization_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> Projects.forOrganization(null, "acme"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void forUser_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> Projects.forUser(sdk, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void forUser_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> Projects.forUser(null, "octocat"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }
}
