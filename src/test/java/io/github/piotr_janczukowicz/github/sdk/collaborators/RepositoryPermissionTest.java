package io.github.piotr_janczukowicz.github.sdk.collaborators;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class RepositoryPermissionTest {

    @ParameterizedTest
    @CsvSource({"PULL,READ", "TRIAGE,TRIAGE", "PUSH,WRITE", "MAINTAIN,MAINTAIN", "ADMIN,ADMIN"})
    void toInvitationUpdate_shouldMapThePermission_whenCalled(
            final RepositoryPermission permission, final GitHubUpdateRepositoryInvitationPermission expected) {
        final GitHubUpdateRepositoryInvitationPermission mapped = permission.toInvitationUpdate();

        assertThat(mapped).isEqualTo(expected);
    }
}
