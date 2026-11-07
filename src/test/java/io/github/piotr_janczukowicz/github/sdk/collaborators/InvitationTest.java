package io.github.piotr_janczukowicz.github.sdk.collaborators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitation;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitationBuilder;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPayload;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.CollaboratorsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class InvitationTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = CollaboratorsFixtures.INVITATION_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRepositoryInvitation response = CollaboratorsFixtures.invitation();
    private final Invitation sut = new Invitation(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenInvitationIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.invitee()).containsSame(response.invitee());
            softly.assertThat(sut.inviter()).containsSame(response.inviter());
            softly.assertThat(sut.permission()).isEqualTo(GitHubRepositoryInvitationPermission.WRITE);
            softly.assertThat(sut.isExpired()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void invitee_shouldBeEmpty_whenTheResponseHasNoInvitee() {
        final Invitation invitation = bareInvitation();

        final Optional<GitHubSimpleUser> invitee = invitation.invitee();

        assertThat(invitee).isEmpty();
    }

    @Test
    void inviter_shouldBeEmpty_whenTheResponseHasNoInviter() {
        final Invitation invitation = bareInvitation();

        final Optional<GitHubSimpleUser> inviter = invitation.inviter();

        assertThat(inviter).isEmpty();
    }

    @Test
    void isExpired_shouldBeFalse_whenTheResponseHasNoExpiryFlag() {
        final Invitation invitation = bareInvitation();

        final boolean isExpired = invitation.isExpired();

        assertThat(isExpired).isFalse();
    }

    private Invitation bareInvitation() {
        final GitHubRepositoryInvitation bare = GitHubRepositoryInvitationBuilder.builder(response)
                .invitee(null)
                .inviter(null)
                .expired(null)
                .build();
        return new Invitation(repository, bare);
    }

    @Test
    void isExpired_shouldBeTrue_whenGitHubReportsItExpired() {
        final GitHubRepositoryInvitation expired = GitHubRepositoryInvitationBuilder.builder(response)
                .expired(true)
                .build();

        final boolean isExpired = new Invitation(repository, expired).isExpired();

        assertThat(isExpired).isTrue();
    }

    @Test
    void changePermission_shouldSendTheMappedPermissionAndReturnTheUpdatedInvitation_whenCalled() throws IOException {
        final GitHubRepositoryInvitation updated = GitHubRepositoryInvitationBuilder.builder(response)
                .permissions(GitHubRepositoryInvitationPermission.READ)
                .build();
        when(github.collaborators().invitations().updateInvitation(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateRepositoryInvitationPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepositoryInvitationPayload.class);

        final Invitation result = sut.changePermission(RepositoryPermission.PULL);

        verify(github.collaborators().invitations()).updateInvitation(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().permissions())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.READ);
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void changePermission_shouldThrowNullPointerException_whenPermissionIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.changePermission(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("permission");
    }

    @Test
    void cancel_shouldDeleteTheInvitation_whenCalled() throws IOException {
        sut.cancel();

        verify(github.collaborators().invitations()).deleteInvitation(OWNER, NAME, ID);
    }
}
