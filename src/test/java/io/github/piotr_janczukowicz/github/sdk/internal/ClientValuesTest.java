package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationMembershipRole;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamRole;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatusState;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.TeamRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class ClientValuesTest {

    @Test
    void reactionToClient_shouldMapEveryContentToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(ReactionContent.values())
                .allMatch(
                        content -> ClientValues.reactionToClient(content).name().equals(content.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void reactionFromClient_shouldMapEveryClientValueBack_whenCalled() {
        final boolean isEveryValueRestored = Arrays.stream(GitHubReactionContent.values())
                .allMatch(
                        content -> ClientValues.reactionToClient(ClientValues.reactionFromClient(content)) == content);

        assertThat(isEveryValueRestored).isTrue();
    }

    @Test
    void reactionValues_shouldMatchTheClientValues_whenCompared() {
        assertThat(ReactionContent.values()).hasSameSizeAs(GitHubReactionContent.values());
    }

    @Test
    void statusStateToClient_shouldMapEveryStateToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(CommitStatusState.values())
                .allMatch(
                        state -> ClientValues.statusStateToClient(state).name().equals(state.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(CommitStatusState.values()).hasSameSizeAs(GitHubCommitStatusState.values());
    }

    @Test
    void permissionToClient_shouldMapEveryPermissionToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(RepositoryPermission.values())
                .allMatch(permission ->
                        ClientValues.permissionToClient(permission).name().equals(permission.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void organizationRoleToClient_shouldMapEveryRoleToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(OrganizationRole.values())
                .allMatch(role ->
                        ClientValues.organizationRoleToClient(role).name().equals(role.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(OrganizationRole.values()).hasSameSizeAs(GitHubOrganizationMembershipRole.values());
    }

    @Test
    void invitationRoleToClient_shouldMapEveryRoleToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(InvitationRole.values())
                .allMatch(
                        role -> ClientValues.invitationRoleToClient(role).name().equals(role.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void teamRoleToClient_shouldMapEveryRoleToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(TeamRole.values())
                .allMatch(role -> ClientValues.teamRoleToClient(role).name().equals(role.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(TeamRole.values()).hasSameSizeAs(GitHubTeamRole.values());
    }

    @Test
    void variableVisibilityToClient_shouldMapEveryVisibilityToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(VariableVisibility.values())
                .allMatch(visibility -> ClientValues.variableVisibilityToClient(visibility)
                        .name()
                        .equals(visibility.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<ClientValues> constructor = ClientValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
