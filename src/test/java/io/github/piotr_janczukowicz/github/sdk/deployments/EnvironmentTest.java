package io.github.piotr_janczukowicz.github.sdk.deployments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentsEnvironmentsApi;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironment;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.DeploymentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class EnvironmentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubEnvironment response = DeploymentFixtures.environment();
    private final Environment sut = new Environment(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenEnvironmentIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo("staging");
            softly.assertThat(sut.htmlUrl()).endsWith("staging");
            softly.assertThat(sut.protectionRules()).isEmpty();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void protectionRules_shouldBeEmpty_whenGitHubReportsNone() {
        final Environment bare = new Environment(repository, DeploymentFixtures.bareEnvironment());

        assertThat(bare.protectionRules()).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedEnvironment_whenItExists() throws IOException {
        final GitHubEnvironment reloaded = DeploymentFixtures.environment();
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(Optional.of(reloaded)).when(api).getEnvironment(OWNER, NAME, "staging");

        final Environment result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheEnvironmentIsGone() throws IOException {
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(Optional.empty()).when(api).getEnvironment(OWNER, NAME, "staging");

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheEnvironment_whenCalled() throws IOException {
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();

        sut.delete();

        verify(api).deleteAnEnvironment(OWNER, NAME, "staging");
    }
}
