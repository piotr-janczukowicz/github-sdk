package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.sdk.Organization;
import io.github.piotr_janczukowicz.github.sdk.Team;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class OrganizationsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void organization_shouldBeEmpty_whenTheLoginDoesNotExist() throws IOException {
        final Optional<Organization> organization = ENV.sdk().organization("smoke-" + ENV.runId() + "-no-such-org");

        assertThat(organization).isEmpty();
    }

    @Test
    void organizations_shouldBeLoadedForTheCurrentUser_whenTheyAreListed() throws IOException {
        final List<Organization> organizations =
                ENV.sdk().currentUser().organizations().limit(3).toList();

        Assumptions.assumeFalse(organizations.isEmpty(), "the current user belongs to no public organization");
        assertThat(organizations).hasSizeLessThanOrEqualTo(3);
        assertThat(organizations)
                .allSatisfy(organization -> assertThat(organization.login()).isNotBlank());
    }

    @Test
    void organization_shouldListRepositoriesMembersAndTeams_whenTheSandboxOwnerIsAnOrganization() throws IOException {
        final Optional<Organization> owner = ENV.sdk().organization(ENV.owner());
        Assumptions.assumeTrue(owner.isPresent(), ENV.owner() + " is not an organization");
        final Organization organization = owner.orElseThrow();

        final boolean hasSandbox = organization
                .repositories()
                .limit(100)
                .anyMatch(repository -> repository.name().equals(ENV.repo()));
        final List<Team> teams = organization.teams().limit(3).toList();

        assertThat(organization.login()).isEqualToIgnoringCase(ENV.owner());
        assertThat(hasSandbox).isTrue();
        assertThat(organization.members().limit(3).toList()).hasSizeLessThanOrEqualTo(3);
        assertThat(teams).allSatisfy(team -> assertThat(team.slug()).isNotBlank());
    }
}
