package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabel;
import java.util.List;

public final class RunnerFixtures {

    public static final long RUNNER_ID = 101L;

    private RunnerFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubRunner runner() {
        final GitHubRunnerLabel label = mock(GitHubRunnerLabel.class);
        final GitHubRunner runner = mock(GitHubRunner.class);
        lenient().when(runner.id()).thenReturn(RUNNER_ID);
        lenient().when(runner.name()).thenReturn("build-1");
        lenient().when(runner.os()).thenReturn("linux");
        lenient().when(runner.status()).thenReturn("online");
        lenient().when(runner.busy()).thenReturn(true);
        lenient().when(runner.labels()).thenReturn(List.of(label));
        return runner;
    }
}
