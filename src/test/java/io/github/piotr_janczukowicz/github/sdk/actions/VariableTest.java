package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsVariablesApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubUpdateRepoVariablePayload;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariable;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariableBuilder;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class VariableTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String VARIABLE = "DEPLOY_ENV";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubActionsVariable response = ActionsFixtures.variable();
    private final Variable sut = new Variable(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenVariableIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo(VARIABLE);
            softly.assertThat(sut.value()).isEqualTo("staging");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedVariable_whenItExists() throws IOException {
        final GitHubActionsVariable reloaded = GitHubActionsVariableBuilder.builder(response)
                .value("production")
                .build();
        final GitHubActionsVariablesApi variables = github.actions().variables();
        doReturn(Optional.of(reloaded)).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Variable result = sut.refresh();

        assertThat(result.value()).isEqualTo("production");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheVariableIsGone() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();
        doReturn(Optional.empty()).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void update_shouldSendTheValueAndReturnTheVariable_whenCalled() throws IOException {
        final GitHubActionsVariable reloaded = GitHubActionsVariableBuilder.builder(response)
                .value("production")
                .build();
        final GitHubActionsVariablesApi variables = github.actions().variables();
        doReturn(Optional.of(reloaded)).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Variable result = sut.update("production");

        assertThat(result.value()).isEqualTo("production");
        verify(variables)
                .updateRepoVariable(OWNER, NAME, VARIABLE, new GitHubUpdateRepoVariablePayload(VARIABLE, "production"));
    }

    @Test
    void update_shouldThrowNullPointerException_whenValueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void delete_shouldDeleteTheVariable_whenCalled() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();

        sut.delete();

        verify(variables).deleteRepoVariable(OWNER, NAME, VARIABLE);
    }
}
