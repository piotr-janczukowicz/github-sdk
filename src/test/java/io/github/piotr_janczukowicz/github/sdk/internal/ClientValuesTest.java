package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReleaseReactionContent;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class ClientValuesTest {

    @Test
    void releaseReactionToClient_shouldMapTheSixAcceptedEmojis_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(GitHubReleaseReactionContent.values())
                .allMatch(content -> ClientValues.releaseReactionToClient(GitHubReactionContent.valueOf(content.name()))
                        .name()
                        .equals(content.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void releaseReactionToClient_shouldThrowIllegalArgumentException_whenTheEmojiIsConfused() {
        final Throwable thrown =
                catchThrowable(() -> ClientValues.releaseReactionToClient(GitHubReactionContent.CONFUSED));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CONFUSED");
    }

    @Test
    void permissionToClient_shouldMapEveryPermissionToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(RepositoryPermission.values())
                .allMatch(permission ->
                        ClientValues.permissionToClient(permission).name().equals(permission.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void invitationRoleToClient_shouldMapEveryRoleToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(InvitationRole.values())
                .allMatch(
                        role -> ClientValues.invitationRoleToClient(role).name().equals(role.name()));

        assertThat(isEveryNameKept).isTrue();
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

    @Test
    void releaseReactionToClient_shouldThrowIllegalArgumentException_whenTheEmojiIsMinusOne() {
        final Throwable thrown =
                catchThrowable(() -> ClientValues.releaseReactionToClient(GitHubReactionContent.MINUS_ONE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MINUS_ONE");
    }
}
