package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubAddCollaboratorPayload;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaborator;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubListCollaboratorsFilter;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryCollaboratorPermission;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitation;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Collaborator;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Invitation;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.fixtures.CollaboratorsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryCollaboratorsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void collaborators_shouldStreamTheCollaborators_whenTheyExist() throws IOException {
        final GitHubCollaborator listed = CollaboratorsFixtures.collaborator();
        when(github.collaborators().collaborator().listCollaborators(eq(OWNER), eq(NAME), any(), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<Collaborator> collaborators = sut.collaborators().toList();

        assertThat(collaborators)
                .singleElement()
                .satisfies(collaborator -> assertSoftly(softly -> {
                    softly.assertThat(collaborator.response()).isSameAs(listed);
                    softly.assertThat(collaborator.repository()).isSameAs(sut);
                }));
    }

    @Test
    void collaborators_shouldPassTheFilterOn_whenOneIsGiven() throws IOException {
        final GitHubListCollaboratorsFilter filter = mock(GitHubListCollaboratorsFilter.class);
        when(github.collaborators().collaborator().listCollaborators(eq(OWNER), eq(NAME), eq(filter), any()).stream())
                .thenReturn(Stream.of(CollaboratorsFixtures.collaborator()));

        final List<Collaborator> collaborators = sut.collaborators(filter).toList();

        assertThat(collaborators).hasSize(1);
    }

    @Test
    void collaborators_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.collaborators(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void isCollaborator_shouldReturnTrue_whenGitHubAnswersYes() throws IOException {
        when(github.collaborators().collaborator().checkCollaborator(OWNER, NAME, "hubot"))
                .thenReturn(true);

        final boolean isMember = sut.isCollaborator("hubot");

        assertThat(isMember).isTrue();
    }

    @Test
    void isCollaborator_shouldReturnFalse_whenGitHubAnswersNo() throws IOException {
        when(github.collaborators().collaborator().checkCollaborator(OWNER, NAME, "stranger"))
                .thenReturn(false);

        final boolean isMember = sut.isCollaborator("stranger");

        assertThat(isMember).isFalse();
    }

    @Test
    void isCollaborator_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.isCollaborator(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void collaboratorPermission_shouldReturnTheLevel_whenGitHubHasIt() throws IOException {
        final GitHubRepositoryCollaboratorPermission level = mock(GitHubRepositoryCollaboratorPermission.class);
        when(github.collaborators().collaborator().getCollaboratorPermissionLevel(OWNER, NAME, "hubot"))
                .thenReturn(Optional.of(level));

        final Optional<GitHubRepositoryCollaboratorPermission> found = sut.collaboratorPermission("hubot");

        assertThat(found).containsSame(level);
    }

    @Test
    void collaboratorPermission_shouldBeEmpty_whenGitHubDoesNotHaveTheUser() throws IOException {
        when(github.collaborators().collaborator().getCollaboratorPermissionLevel(OWNER, NAME, "stranger"))
                .thenReturn(Optional.empty());

        final Optional<GitHubRepositoryCollaboratorPermission> found = sut.collaboratorPermission("stranger");

        assertThat(found).isEmpty();
    }

    @Test
    void addCollaborator_shouldReturnTheInvitation_whenGitHubInvitesTheUser() throws IOException {
        final GitHubRepositoryInvitation invited = CollaboratorsFixtures.invitation();
        when(github.collaborators().collaborator().addCollaborator(OWNER, NAME, "hubot"))
                .thenReturn(Optional.of(invited));

        final Optional<Invitation> invitation = sut.addCollaborator("hubot");

        assertThat(invitation)
                .hasValueSatisfying(result -> assertSoftly(softly -> {
                    softly.assertThat(result.response()).isSameAs(invited);
                    softly.assertThat(result.repository()).isSameAs(sut);
                }));
    }

    @Test
    void addCollaborator_shouldBeEmpty_whenGitHubAddsTheUserWithoutInvitation() throws IOException {
        when(github.collaborators().collaborator().addCollaborator(OWNER, NAME, "member"))
                .thenReturn(Optional.empty());

        final Optional<Invitation> invitation = sut.addCollaborator("member");

        assertThat(invitation).isEmpty();
    }

    @Test
    void addCollaborator_shouldSendThePermissionWireValue_whenOneIsGiven() throws IOException {
        when(github.collaborators().collaborator().addCollaborator(eq(OWNER), eq(NAME), eq("hubot"), any()))
                .thenReturn(Optional.of(CollaboratorsFixtures.invitation()));
        final ArgumentCaptor<GitHubAddCollaboratorPayload> payload =
                ArgumentCaptor.forClass(GitHubAddCollaboratorPayload.class);

        sut.addCollaborator("hubot", RepositoryPermission.PUSH);

        verify(github.collaborators().collaborator())
                .addCollaborator(eq(OWNER), eq(NAME), eq("hubot"), payload.capture());
        assertThat(payload.getValue().permission()).isEqualTo("push");
    }

    @Test
    void addCollaborator_shouldThrowNullPointerException_whenPermissionIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addCollaborator("hubot", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("permission");
    }

    @Test
    void addCollaborator_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addCollaborator(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void invitations_shouldStreamTheInvitations_whenTheyExist() throws IOException {
        final GitHubRepositoryInvitation listed = CollaboratorsFixtures.invitation();
        when(github.collaborators().invitations().listInvitations(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));

        final List<Invitation> invitations = sut.invitations().toList();

        assertThat(invitations)
                .singleElement()
                .satisfies(invitation -> assertSoftly(softly -> {
                    softly.assertThat(invitation.response()).isSameAs(listed);
                    softly.assertThat(invitation.repository()).isSameAs(sut);
                }));
    }
}
