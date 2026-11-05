package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamApi;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamMembership;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamRole;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamsMembersApi;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpdateTeamPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpsertTeamMembershipPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpsertTeamRepositoryPermissionPayload;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.fixtures.OrganizationFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class TeamManagementTest {

    private static final String ORG = OrganizationFixtures.ORG;
    private static final String SLUG = OrganizationFixtures.SLUG;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Organization organization = organization();
    private final Team sut = team(organization);
    private final Repository repository = sdk.repository("octocat", "hello-world");

    @Test
    void childTeams_shouldLoadEachListedTeam_whenTheyExist() throws IOException {
        final GitHubTeam listed = OrganizationFixtures.listedTeam("backend");
        final GitHubTeamFull loaded = mock(GitHubTeamFull.class);
        when(github.teams().team().listChildTeams(ORG, SLUG).stream()).thenReturn(Stream.of(listed));
        final GitHubTeamApi api = github.teams().team();
        doReturn(Optional.of(loaded)).when(api).getByName(ORG, "backend");

        final List<Team> children = sut.childTeams().toList();

        assertThat(children)
                .singleElement()
                .satisfies(child -> assertThat(child.response()).isSameAs(loaded));
    }

    @Test
    void childTeams_shouldSkipTheTeam_whenAListedTeamIsGone() throws IOException {
        final GitHubTeam listed = OrganizationFixtures.listedTeam("backend");
        when(github.teams().team().listChildTeams(ORG, SLUG).stream()).thenReturn(Stream.of(listed));
        final GitHubTeamApi api = github.teams().team();
        doReturn(Optional.empty()).when(api).getByName(ORG, "backend");

        final List<Team> children = sut.childTeams().toList();

        assertThat(children).isEmpty();
    }

    @Test
    void update_shouldSendThePayloadAndReturnTheTeam_whenCalled() throws IOException {
        final GitHubUpdateTeamPayload payload = mock(GitHubUpdateTeamPayload.class);
        final GitHubTeamFull updated = mock(GitHubTeamFull.class);
        final GitHubTeamApi api = github.teams().team();
        doReturn(updated).when(api).update(ORG, SLUG, payload);

        final Team result = sut.update(payload);

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(result.organization()).isSameAs(organization);
        });
    }

    @Test
    void update_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void delete_shouldDeleteTheTeam_whenCalled() throws IOException {
        final GitHubTeamApi api = github.teams().team();

        sut.delete();

        verify(api).delete(ORG, SLUG);
    }

    @Test
    void addMember_shouldAddAsRegularMember_whenNoRoleIsGiven() throws IOException {
        final GitHubTeamMembership membership = mock(GitHubTeamMembership.class);
        final GitHubTeamsMembersApi api = github.teams().members();
        doReturn(membership).when(api).upsertMembership(ORG, SLUG, "hubot");

        final GitHubTeamMembership result = sut.addMember("hubot");

        assertThat(result).isSameAs(membership);
    }

    @Test
    void addMember_shouldSendTheRole_whenOneIsGiven() throws IOException {
        final GitHubTeamsMembersApi api = github.teams().members();
        doReturn(mock(GitHubTeamMembership.class))
                .when(api)
                .upsertMembership(eq(ORG), eq(SLUG), eq("hubot"), any(GitHubUpsertTeamMembershipPayload.class));
        final ArgumentCaptor<GitHubUpsertTeamMembershipPayload> payload =
                ArgumentCaptor.forClass(GitHubUpsertTeamMembershipPayload.class);

        sut.addMember("hubot", GitHubTeamRole.MAINTAINER);

        verify(api).upsertMembership(eq(ORG), eq(SLUG), eq("hubot"), payload.capture());
        assertThat(payload.getValue().role()).isEqualTo(GitHubTeamRole.MAINTAINER);
    }

    @Test
    void addMember_shouldThrowNullPointerException_whenRoleIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addMember("hubot", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("role");
    }

    @Test
    void addMember_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addMember(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void removeMember_shouldRemoveTheUser_whenCalled() throws IOException {
        final GitHubTeamsMembersApi api = github.teams().members();

        sut.removeMember("hubot");

        verify(api).removeMembership(ORG, SLUG, "hubot");
    }

    @Test
    void membership_shouldReturnTheMembership_whenTheUserHasOne() throws IOException {
        final GitHubTeamMembership membership = mock(GitHubTeamMembership.class);
        final GitHubTeamsMembersApi api = github.teams().members();
        doReturn(Optional.of(membership)).when(api).getMembership(ORG, SLUG, "hubot");

        final Optional<GitHubTeamMembership> found = sut.membership("hubot");

        assertThat(found).containsSame(membership);
    }

    @Test
    void membership_shouldBeEmpty_whenTheUserHasNone() throws IOException {
        final GitHubTeamsMembersApi api = github.teams().members();
        doReturn(Optional.empty()).when(api).getMembership(ORG, SLUG, "hubot");

        final Optional<GitHubTeamMembership> found = sut.membership("hubot");

        assertThat(found).isEmpty();
    }

    @Test
    void addRepository_shouldGiveTheDefaultPermission_whenNoPermissionIsGiven() throws IOException {
        final GitHubTeamApi api = github.teams().team();

        sut.addRepository(repository);

        verify(api).upsertRepositoryPermission(ORG, SLUG, "octocat", "hello-world");
    }

    @Test
    void addRepository_shouldSendThePermission_whenOneIsGiven() throws IOException {
        final GitHubTeamApi api = github.teams().team();
        final ArgumentCaptor<GitHubUpsertTeamRepositoryPermissionPayload> payload =
                ArgumentCaptor.forClass(GitHubUpsertTeamRepositoryPermissionPayload.class);

        sut.addRepository(repository, RepositoryPermission.MAINTAIN);

        verify(api).upsertRepositoryPermission(eq(ORG), eq(SLUG), eq("octocat"), eq("hello-world"), payload.capture());
        assertThat(payload.getValue().permission()).isEqualTo("maintain");
    }

    @Test
    void addRepository_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addRepository(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void addRepository_shouldThrowNullPointerException_whenPermissionIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addRepository(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("permission");
    }

    @Test
    void removeRepository_shouldTakeTheRepositoryAway_whenCalled() throws IOException {
        final GitHubTeamApi api = github.teams().team();

        sut.removeRepository(repository);

        verify(api).removeRepository(ORG, SLUG, "octocat", "hello-world");
    }

    @Test
    void removeRepository_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.removeRepository(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn(ORG);
        return new Organization(sdk, response);
    }

    private Team team(final Organization parent) {
        final GitHubTeamFull response = mock(GitHubTeamFull.class);
        lenient().when(response.slug()).thenReturn(SLUG);
        return new Team(parent, response);
    }
}
