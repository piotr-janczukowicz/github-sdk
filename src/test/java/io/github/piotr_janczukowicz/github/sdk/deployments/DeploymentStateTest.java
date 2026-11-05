package io.github.piotr_janczukowicz.github.sdk.deployments;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class DeploymentStateTest {

    @Test
    void toClient_shouldMapEveryStateToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(DeploymentState.values())
                .allMatch(state -> state.toClient().name().equals(state.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(DeploymentState.values()).hasSameSizeAs(GitHubDeploymentStatusState.values());
    }
}
