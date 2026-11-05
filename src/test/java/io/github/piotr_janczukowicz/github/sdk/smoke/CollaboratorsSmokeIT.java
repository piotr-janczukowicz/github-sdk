package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Collaborator;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Invitation;
import java.io.IOException;
import java.util.List;
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
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<Collaborator> collaborators = repository.collaborators().toList();
        final boolean isOwnerCollaborator = repository.isCollaborator(ENV.owner());
        final boolean isStrangerCollaborator = repository.isCollaborator("smoke-" + ENV.runId() + "-nobody");

        assertSoftly(softly -> {
            softly.assertThat(collaborators)
                    .extracting(Collaborator::login)
                    .anySatisfy(login -> assertThat(login).isEqualToIgnoringCase(ENV.owner()));
            softly.assertThat(isOwnerCollaborator).isTrue();
            softly.assertThat(isStrangerCollaborator).isFalse();
        });
    }

    @Test
    void collaboratorPermission_shouldBeAdmin_whenTheUserIsTheOwner() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final String permission =
                repository.collaboratorPermission(ENV.owner()).orElseThrow().permission();

        assertThat(permission).isEqualTo("admin");
    }

    @Test
    void invitations_shouldBeListed_whenTheRepositoryHasNoneOrSome() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<Invitation> invitations = repository.invitations().limit(5).toList();

        assertThat(invitations)
                .allSatisfy(invitation -> assertThat(invitation.id()).isPositive());
    }
}
