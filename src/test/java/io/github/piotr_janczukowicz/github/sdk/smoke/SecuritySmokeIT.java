package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
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

/** Reads only: no alert is dismissed or resolved and no switch is changed, so nothing real is altered. */
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

        assertThat(alerts).allSatisfy(alert -> {
            assertThat(alert.summary()).isNotBlank();
            assertThat(alert.refresh().number()).isEqualTo(alert.number());
        });
    }

    private static List<DependabotAlert> readDependabot(final Repository repository) throws IOException {
        try {
            return repository.dependabotAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException | GitHubNotFoundException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void secretScanningAlerts_shouldBeListed_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<SecretScanningAlert> alerts = readSecrets(repository);

        assertThat(alerts).allSatisfy(alert -> {
            assertThat(alert.secretType())
                    .hasValueSatisfying(type -> assertThat(type).isNotBlank());
            assertThat(alert.locations().limit(1).count()).isLessThanOrEqualTo(1);
        });
    }

    private static List<SecretScanningAlert> readSecrets(final Repository repository) throws IOException {
        try {
            return repository.secretScanningAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException | GitHubNotFoundException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void codeScanningAlerts_shouldBeListed_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<CodeScanningAlert> alerts = readCode(repository);

        assertThat(alerts).allSatisfy(alert -> assertThat(alert.ruleId()).isNotBlank());
    }

    private static List<CodeScanningAlert> readCode(final Repository repository) throws IOException {
        try {
            return repository.codeScanningAlerts().limit(3).toList();
        } catch (final GitHubForbiddenException | GitHubNotFoundException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }

    @Test
    void securitySwitches_shouldBeReadable_whenTheTokenAllowsIt() throws IOException {
        final RepositorySecurity security =
                ENV.sdk().repository(ENV.owner(), ENV.repo()).security();

        final boolean isRead = readSwitches(security);

        assertThat(isRead).isTrue();
    }

    private static boolean readSwitches(final RepositorySecurity security) throws IOException {
        try {
            security.isVulnerabilityAlertsEnabled();
            security.isAutomatedSecurityFixesEnabled();
            security.isPrivateVulnerabilityReportingEnabled();
            security.isImmutableReleasesEnabled();
            return true;
        } catch (final GitHubForbiddenException | GitHubNotFoundException e) {
            Assumptions.abort(NO_ACCESS);
            throw e;
        }
    }
}
