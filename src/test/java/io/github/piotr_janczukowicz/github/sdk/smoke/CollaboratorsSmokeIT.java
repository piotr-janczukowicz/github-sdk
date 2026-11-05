package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Collaborator;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Invitation;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class CollaboratorsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void collaborators_shouldListTheOwner_whenTheSandboxIsAPersonalRepository() throws IOException {
        assumePersonalSandbox();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<Collaborator> collaborators = repository.collaborators().toList();

        assertThat(collaborators)
                .extracting(Collaborator::login)
                .anySatisfy(login -> assertThat(login).isEqualToIgnoringCase(ENV.owner()));
    }

    @Test
    void isCollaborator_shouldBeTrue_whenTheUserIsTheOwnerOfAPersonalSandbox() throws IOException {
        assumePersonalSandbox();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean isOwnerCollaborator = repository.isCollaborator(ENV.owner());

        assertThat(isOwnerCollaborator).isTrue();
    }

    @Test
    void isCollaborator_shouldBeFalse_whenTheUserIsAStranger() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean isStrangerCollaborator = repository.isCollaborator("smoke-" + ENV.runId() + "-nobody");

        assertThat(isStrangerCollaborator).isFalse();
    }

    private static void assumePersonalSandbox() throws IOException {
        final String login = ENV.sdk().currentUser().login();
        Assumptions.assumeTrue(login.equalsIgnoreCase(ENV.owner()), "The sandbox is not a personal repository");
    }

    @Test
    void collaboratorPermission_shouldBeAdmin_whenTheUserIsTheOwner() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final String permission =
                repository.collaboratorPermission(ENV.owner()).orElseThrow().permission();

        assertThat(permission).isEqualTo("admin");
    }

    @Test
    void invitations_shouldBeListed_whenTheRepositoryHasSome() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final List<Invitation> invitations = listInvitations(repository);
        Assumptions.assumeFalse(invitations.isEmpty(), "The sandbox has no invitations");

        assertThat(invitations)
                .extracting(Invitation::id)
                .allSatisfy(id -> assertThat(id).isPositive());
    }

    private static List<Invitation> listInvitations(final Repository repository) throws IOException {
        try {
            return repository.invitations().limit(5).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Administration permission, so GitHub refuses listing invitations");
            throw e;
        }
    }
}
