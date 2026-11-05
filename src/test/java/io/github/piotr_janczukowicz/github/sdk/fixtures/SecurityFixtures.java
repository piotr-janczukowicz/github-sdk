package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlert;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertRule;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertState;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlert;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertDependency;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertPackage;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertSecurityAdvisory;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertSeverity;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertState;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertResolution;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertState;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertWithMetadata;

public final class SecurityFixtures {

    public static final int DEPENDABOT_NUMBER = 7;
    public static final int SECRET_NUMBER = 5;
    public static final int CODE_NUMBER = 3;

    private static final String HTML = "https://github.com/octocat/hello-world/security/";

    private SecurityFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubDependabotAlert dependabotAlert() {
        return dependabotAlertWith(GitHubDependabotAlertState.OPEN);
    }

    public static GitHubDependabotAlert dependabotAlertWith(final GitHubDependabotAlertState state) {
        final GitHubDependabotAlertPackage dependencyPackage = mock(GitHubDependabotAlertPackage.class);
        lenient().when(dependencyPackage.name()).thenReturn("lodash");
        final GitHubDependabotAlertDependency dependency = mock(GitHubDependabotAlertDependency.class);
        lenient().when(dependency.dependencyPackage()).thenReturn(dependencyPackage);
        final GitHubDependabotAlertSecurityAdvisory advisory = mock(GitHubDependabotAlertSecurityAdvisory.class);
        lenient().when(advisory.severity()).thenReturn(GitHubDependabotAlertSeverity.HIGH);
        lenient().when(advisory.summary()).thenReturn("Prototype pollution");
        final GitHubDependabotAlert alert = mock(GitHubDependabotAlert.class);
        lenient().when(alert.number()).thenReturn(DEPENDABOT_NUMBER);
        lenient().when(alert.state()).thenReturn(state);
        lenient().when(alert.dependency()).thenReturn(dependency);
        lenient().when(alert.securityAdvisory()).thenReturn(advisory);
        lenient().when(alert.htmlUrl()).thenReturn(HTML + "dependabot/7");
        return alert;
    }

    public static GitHubDependabotAlert dependabotAlertWithoutDependency() {
        final GitHubDependabotAlert alert = mock(GitHubDependabotAlert.class);
        lenient().when(alert.number()).thenReturn(DEPENDABOT_NUMBER);
        lenient().when(alert.dependency()).thenReturn(null);
        return alert;
    }

    public static GitHubDependabotAlert dependabotAlertWithoutPackage() {
        final GitHubDependabotAlertDependency dependency = mock(GitHubDependabotAlertDependency.class);
        lenient().when(dependency.dependencyPackage()).thenReturn(null);
        final GitHubDependabotAlert alert = mock(GitHubDependabotAlert.class);
        lenient().when(alert.number()).thenReturn(DEPENDABOT_NUMBER);
        lenient().when(alert.dependency()).thenReturn(dependency);
        return alert;
    }

    public static GitHubSecretScanningAlertWithMetadata secretAlert() {
        return secretAlertWith(GitHubSecretScanningAlertState.OPEN, null, true);
    }

    public static GitHubSecretScanningAlertWithMetadata secretAlertWith(
            final GitHubSecretScanningAlertState state,
            final GitHubSecretScanningAlertResolution resolution,
            final Boolean isLeaked) {
        final GitHubSecretScanningAlertWithMetadata alert = mock(GitHubSecretScanningAlertWithMetadata.class);
        lenient().when(alert.number()).thenReturn(SECRET_NUMBER);
        lenient().when(alert.state()).thenReturn(state);
        lenient().when(alert.resolution()).thenReturn(resolution);
        lenient().when(alert.secretType()).thenReturn("github_personal_access_token");
        lenient().when(alert.publiclyLeaked()).thenReturn(isLeaked);
        lenient().when(alert.htmlUrl()).thenReturn(HTML + "secret-scanning/5");
        return alert;
    }

    public static GitHubCodeScanningAlert codeAlert() {
        return codeAlertWith(GitHubCodeScanningAlertState.OPEN);
    }

    public static GitHubCodeScanningAlert codeAlertWith(final GitHubCodeScanningAlertState state) {
        final GitHubCodeScanningAlertRule rule = mock(GitHubCodeScanningAlertRule.class);
        lenient().when(rule.id()).thenReturn("java/sql-injection");
        lenient().when(rule.name()).thenReturn("SQL injection");
        final GitHubCodeScanningAlert alert = mock(GitHubCodeScanningAlert.class);
        lenient().when(alert.number()).thenReturn(CODE_NUMBER);
        lenient().when(alert.state()).thenReturn(state);
        lenient().when(alert.rule()).thenReturn(rule);
        lenient().when(alert.htmlUrl()).thenReturn(HTML + "code-scanning/3");
        return alert;
    }
}
