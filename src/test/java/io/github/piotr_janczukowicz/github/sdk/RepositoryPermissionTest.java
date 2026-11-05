package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaboratorPermission;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryPermissionTest {

    @Test
    void toClient_shouldMapEveryPermissionToTheClientValue_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(RepositoryPermission.PULL.toClient()).isEqualTo(GitHubCollaboratorPermission.PULL);
            softly.assertThat(RepositoryPermission.TRIAGE.toClient()).isEqualTo(GitHubCollaboratorPermission.TRIAGE);
            softly.assertThat(RepositoryPermission.PUSH.toClient()).isEqualTo(GitHubCollaboratorPermission.PUSH);
            softly.assertThat(RepositoryPermission.MAINTAIN.toClient())
                    .isEqualTo(GitHubCollaboratorPermission.MAINTAIN);
            softly.assertThat(RepositoryPermission.ADMIN.toClient()).isEqualTo(GitHubCollaboratorPermission.ADMIN);
        });
    }

    @Test
    void toInvitationUpdate_shouldMapPullToReadAndPushToWrite_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(RepositoryPermission.PULL.toInvitationUpdate())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.READ);
            softly.assertThat(RepositoryPermission.TRIAGE.toInvitationUpdate())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.TRIAGE);
            softly.assertThat(RepositoryPermission.PUSH.toInvitationUpdate())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.WRITE);
            softly.assertThat(RepositoryPermission.MAINTAIN.toInvitationUpdate())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.MAINTAIN);
            softly.assertThat(RepositoryPermission.ADMIN.toInvitationUpdate())
                    .isEqualTo(GitHubUpdateRepositoryInvitationPermission.ADMIN);
        });
    }
}
