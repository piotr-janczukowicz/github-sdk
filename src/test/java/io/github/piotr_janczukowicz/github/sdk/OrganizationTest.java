package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateInOrgPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.fixtures.OrganizationFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class OrganizationTest {

    private static final String ORG = "octo-org";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
    private final Organization sut = new Organization(sdk, response);

    @Test
    void login_shouldReadTheLoginOfTheResponse_whenCalled() {
        when(response.login()).thenReturn(ORG);

        final String login = sut.login();

        assertThat(login).isEqualTo(ORG);
    }

    @Test
    void name_shouldReturnTheName_whenTheOrganizationHasOne() {
        when(response.name()).thenReturn("Octo Org");

        final Optional<String> name = sut.name();

        assertThat(name).contains("Octo Org");
    }

    @Test
    void name_shouldBeEmpty_whenTheOrganizationHasNone() {
        when(response.name()).thenReturn(null);

        final Optional<String> name = sut.name();

        assertThat(name).isEmpty();
    }

    @Test
    void response_shouldReturnTheResponse_whenCalled() {
        final GitHubOrganizationFull result = sut.response();

        assertThat(result).isSameAs(response);
    }

    @Test
    void repositories_shouldReturnHandlesOfTheListedRepositories_whenTheOrganizationHasRepositories()
            throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn(ORG);
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("hello-world");
        when(github.repositories().repository().listForOrg(ORG).stream()).thenReturn(Stream.of(listed));

        final List<Repository> repositories = sut.repositories().toList();

        assertSoftly(softly -> {
            softly.assertThat(repositories).hasSize(1);
            softly.assertThat(repositories.getFirst().owner()).isEqualTo(ORG);
            softly.assertThat(repositories.getFirst().name()).isEqualTo("hello-world");
        });
    }

    @Test
    void createRepository_shouldReturnTheLoadedRepository_whenGitHubCreatesIt() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubCreateInOrgPayload payload = mock(GitHubCreateInOrgPayload.class);
        final GitHubFullRepository created = mock(GitHubFullRepository.class);
        when(created.name()).thenReturn("new-repo");
        when(github.repositories().repository().createInOrg(ORG, payload)).thenReturn(created);

        final Repository repository = sut.createRepository(payload);

        assertSoftly(softly -> {
            softly.assertThat(repository.owner()).isEqualTo(ORG);
            softly.assertThat(repository.name()).isEqualTo("new-repo");
            softly.assertThat(repository.response()).isSameAs(created);
        });
    }

    @Test
    void createRepository_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createRepository(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void members_shouldStreamTheMembers_whenTheOrganizationHasMembers() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubSimpleUser member = mock(GitHubSimpleUser.class);
        when(github.organizations().members().listMembers(ORG).stream()).thenReturn(Stream.of(member));

        final List<GitHubSimpleUser> members = sut.members().toList();

        assertThat(members).containsExactly(member);
    }

    @Test
    void team_shouldReturnTheTeam_whenGitHubHasTheSlug() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubTeamFull teamResponse = mock(GitHubTeamFull.class);
        when(github.teams().team().getByName(ORG, "core")).thenReturn(Optional.of(teamResponse));

        final Optional<Team> team = sut.team("core");

        assertSoftly(softly -> {
            softly.assertThat(team).isPresent();
            softly.assertThat(team.orElseThrow().response()).isSameAs(teamResponse);
            softly.assertThat(team.orElseThrow().organization()).isSameAs(sut);
        });
    }

    @Test
    void team_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(response.login()).thenReturn(ORG);
        when(github.teams().team().getByName(ORG, "ghost")).thenReturn(Optional.empty());

        final Optional<Team> team = sut.team("ghost");

        assertThat(team).isEmpty();
    }

    @Test
    void team_shouldThrowIllegalArgumentException_whenSlugIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.team(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("slug");
    }

    @Test
    void teams_shouldLoadEachListedTeam_whenTheOrganizationHasTeams() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubTeamFull teamResponse = mock(GitHubTeamFull.class);
        final GitHubTeam listed = OrganizationFixtures.listedTeam("core");
        when(github.teams().team().list(ORG).stream()).thenReturn(Stream.of(listed));
        when(github.teams().team().getByName(ORG, "core")).thenReturn(Optional.of(teamResponse));

        final List<Team> teams = sut.teams().toList();

        assertThat(teams).extracting(Team::response).containsExactly(teamResponse);
    }

    @Test
    void teams_shouldSkipTheItem_whenAListedTeamIsGone() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubTeam listed = OrganizationFixtures.listedTeam("core");
        when(github.teams().team().list(ORG).stream()).thenReturn(Stream.of(listed));
        when(github.teams().team().getByName(ORG, "core")).thenReturn(Optional.empty());
        final Stream<Team> teams = sut.teams();

        final long count = teams.count();

        assertThat(count).isZero();
    }

    @Test
    void teams_shouldThrowUncheckedIoException_whenLoadingAListedTeamFails() throws IOException {
        when(response.login()).thenReturn(ORG);
        final GitHubTeam listed = OrganizationFixtures.listedTeam("core");
        when(github.teams().team().list(ORG).stream()).thenReturn(Stream.of(listed));
        when(github.teams().team().getByName(ORG, "core")).thenThrow(new IOException("transport failed"));
        final Stream<Team> teams = sut.teams();

        final Throwable thrown = catchThrowable(teams::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }
}
