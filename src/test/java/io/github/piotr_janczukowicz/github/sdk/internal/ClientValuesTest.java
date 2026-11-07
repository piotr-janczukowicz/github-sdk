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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    @ParameterizedTest
    @EnumSource(
            value = GitHubReactionContent.class,
            names = {"MINUS_ONE", "CONFUSED"})
    void releaseReactionToClient_shouldThrowIllegalArgumentException_whenTheEmojiIsNotAccepted(
            final GitHubReactionContent content) {
        final Throwable thrown = catchThrowable(() -> ClientValues.releaseReactionToClient(content));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(content.name());
    }

    @Test
    void releaseReactionToClient_shouldThrowNullPointerException_whenTheEmojiIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientValues.releaseReactionToClient(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
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
    void permissionToClient_shouldThrowNullPointerException_whenThePermissionIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientValues.permissionToClient(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void invitationRoleToClient_shouldThrowNullPointerException_whenTheRoleIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientValues.invitationRoleToClient(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void variableVisibilityToClient_shouldThrowNullPointerException_whenTheVisibilityIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientValues.variableVisibilityToClient(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
