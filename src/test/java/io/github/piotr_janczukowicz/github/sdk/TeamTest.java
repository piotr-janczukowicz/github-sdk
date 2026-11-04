package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamMember;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class TeamTest {

    private static final String ORG = "octo-org";
    private static final String SLUG = "core";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubOrganizationFull organizationRecord = mock(GitHubOrganizationFull.class);
    private final Organization organization = new Organization(sdk, organizationRecord);
    private final GitHubTeamFull record = mock(GitHubTeamFull.class);
    private final Team sut = new Team(organization, record);

    @Test
    void accessors_shouldExposeTheRecord_whenTeamIsLoaded() {
        when(record.slug()).thenReturn(SLUG);
        when(record.name()).thenReturn("Core team");

        assertSoftly(softly -> {
            softly.assertThat(sut.organization()).isSameAs(organization);
            softly.assertThat(sut.slug()).isEqualTo(SLUG);
            softly.assertThat(sut.name()).isEqualTo("Core team");
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void members_shouldStreamTheMembers_whenTheTeamHasMembers() throws IOException {
        when(organizationRecord.login()).thenReturn(ORG);
        when(record.slug()).thenReturn(SLUG);
        final GitHubTeamMember member = mock(GitHubTeamMember.class);
        when(github.teams().members().listMembers(ORG, SLUG).stream()).thenReturn(Stream.of(member));

        final List<GitHubTeamMember> members = sut.members().toList();

        assertThat(members).containsExactly(member);
    }

    @Test
    void repositories_shouldReturnHandlesOfTheListedRepositories_whenTheTeamHasRepositories() throws IOException {
        when(organizationRecord.login()).thenReturn(ORG);
        when(record.slug()).thenReturn(SLUG);
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn(ORG);
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("hello-world");
        when(github.teams().team().listRepositories(ORG, SLUG).stream()).thenReturn(Stream.of(listed));

        final List<Repository> repositories = sut.repositories().toList();

        assertThat(repositories).hasSize(1);
        assertSoftly(softly -> {
            softly.assertThat(repositories.getFirst().owner()).isEqualTo(ORG);
            softly.assertThat(repositories.getFirst().name()).isEqualTo("hello-world");
        });
    }
}
