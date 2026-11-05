package io.github.piotr_janczukowicz.github.sdk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertDismissedReason;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertDismissedReason;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertResolution;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class DismissalEnumsTest {

    @Test
    void toClient_shouldMapEveryDependabotReasonToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(DependabotDismissal.values())
                .allMatch(reason -> reason.toClient().name().equals(reason.name()));

        assertSoftly(softly -> {
            softly.assertThat(isEveryNameKept).isTrue();
            softly.assertThat(DependabotDismissal.values())
                    .hasSameSizeAs(GitHubDependabotAlertDismissedReason.values());
        });
    }

    @Test
    void toClient_shouldMapEverySecretResolutionToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(SecretResolution.values())
                .allMatch(resolution -> resolution.toClient().name().equals(resolution.name()));

        assertSoftly(softly -> {
            softly.assertThat(isEveryNameKept).isTrue();
            softly.assertThat(SecretResolution.values()).hasSameSizeAs(GitHubSecretScanningAlertResolution.values());
        });
    }

    @Test
    void toClient_shouldMapEveryCodeScanningReasonToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(CodeScanningDismissal.values())
                .allMatch(reason -> reason.toClient().name().equals(reason.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(CodeScanningDismissal.values()).hasSameSizeAs(GitHubCodeScanningAlertDismissedReason.values());
    }
}
