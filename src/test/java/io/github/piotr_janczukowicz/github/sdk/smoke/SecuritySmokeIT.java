package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.security.CodeScanningAlert;
import io.github.piotr_janczukowicz.github.sdk.security.DependabotAlert;
import io.github.piotr_janczukowicz.github.sdk.security.RepositorySecurity;
import io.github.piotr_janczukowicz.github.sdk.security.SecretScanningAlert;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class SecuritySmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String NO_ACCESS = "The token has no permission for this alert type, or the feature is off";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void dependabotAlerts_shouldBeListedAndReloaded_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<DependabotAlert> alerts = readDependabot(repository);

        Assumptions.assumeFalse(alerts.isEmpty(), "The sandbox has no Dependabot alerts");
        final DependabotAlert first = alerts.getFirst();
        final int reloadedNumber = first.refresh().number();

        assertSoftly(softly -> {
            softly.assertThat(first.summary()).isNotBlank();
            softly.assertThat(reloadedNumber).isEqualTo(first.number());
        });
    }

    private static List<DependabotAlert> readDependabot(final Repository repository) throws IOException {
        try {
            return repository.dependabotAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void secretScanningAlerts_shouldBeListed_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<SecretScanningAlert> alerts = readSecrets(repository);

        Assumptions.assumeFalse(alerts.isEmpty(), "The sandbox has no secret scanning alerts");
        assertThat(alerts)
                .allSatisfy(alert -> assertThat(alert.secretType())
                        .hasValueSatisfying(type -> assertThat(type).isNotBlank()));
    }

    private static List<SecretScanningAlert> readSecrets(final Repository repository) throws IOException {
        try {
            return repository.secretScanningAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void codeScanningAlerts_shouldBeListed_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<CodeScanningAlert> alerts = readCode(repository);

        Assumptions.assumeFalse(alerts.isEmpty(), "The sandbox has no code scanning alerts");
        assertThat(alerts).allSatisfy(alert -> assertThat(alert.ruleId()).isPresent());
    }

    private static List<CodeScanningAlert> readCode(final Repository repository) throws IOException {
        try {
            return repository.codeScanningAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void securitySwitches_shouldBeReadable_whenTheTokenAllowsIt() throws IOException {
        final RepositorySecurity security =
                ENV.sdk().repository(ENV.owner(), ENV.repo()).security();

        final List<Boolean> switches = readSwitches(security);

        assertThat(switches).hasSize(4).doesNotContainNull();
    }

    private static List<Boolean> readSwitches(final RepositorySecurity security) throws IOException {
        try {
            return List.of(
                    security.isVulnerabilityAlertsEnabled(),
                    security.isAutomatedSecurityFixesEnabled(),
                    security.isPrivateVulnerabilityReportingEnabled(),
                    security.isImmutableReleasesEnabled());
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }
}
