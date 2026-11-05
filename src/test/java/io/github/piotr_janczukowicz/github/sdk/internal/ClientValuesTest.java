package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatusState;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
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
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<ClientValues> constructor = ClientValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
