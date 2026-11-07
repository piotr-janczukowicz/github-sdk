package io.github.piotr_janczukowicz.github.sdk.security;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutomatedSecurityFixesStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryApi;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * The security switches of a {@link Repository}. Every method asks GitHub or changes the setting at once; the object
 * holds no state, so it never goes stale.
 */
public final class RepositorySecurity {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositorySecurity(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Tells whether Dependabot alerts are on ({@code GET /repos/{owner}/{repo}/vulnerability-alerts}).
     *
     * @return {@code true} when vulnerability alerts are enabled
     * @throws IOException if the transport fails
     */
    public boolean isVulnerabilityAlertsEnabled() throws IOException {
        return api().checkVulnerabilityAlerts(repository.owner(), repository.name());
    }

    /**
     * Turns Dependabot alerts on ({@code PUT /repos/{owner}/{repo}/vulnerability-alerts}).
     *
     * @throws IOException if the transport fails
     */
    public void enableVulnerabilityAlerts() throws IOException {
        api().enableVulnerabilityAlerts(repository.owner(), repository.name());
    }

    /**
     * Turns Dependabot alerts off ({@code DELETE /repos/{owner}/{repo}/vulnerability-alerts}).
     *
     * @throws IOException if the transport fails
     */
    public void disableVulnerabilityAlerts() throws IOException {
        api().disableVulnerabilityAlerts(repository.owner(), repository.name());
    }

    /**
     * Tells whether Dependabot security updates are on ({@code GET /repos/{owner}/{repo}/automated-security-fixes}).
     * They are on but paused when GitHub reports {@code enabled} with {@code paused}; this method counts only an active
     * setting.
     *
     * @return {@code true} when automated security fixes are enabled and not paused
     * @throws IOException if the transport fails
     */
    public boolean isAutomatedSecurityFixesEnabled() throws IOException {
        final GitHubAutomatedSecurityFixesStatus status =
                api().checkAutomatedSecurityFixes(repository.owner(), repository.name());
        return status.enabled() && !status.paused();
    }

    /**
     * Turns Dependabot security updates on ({@code PUT /repos/{owner}/{repo}/automated-security-fixes}).
     *
     * @throws IOException if the transport fails
     */
    public void enableAutomatedSecurityFixes() throws IOException {
        api().enableAutomatedSecurityFixes(repository.owner(), repository.name());
    }

    /**
     * Turns Dependabot security updates off ({@code DELETE /repos/{owner}/{repo}/automated-security-fixes}).
     *
     * @throws IOException if the transport fails
     */
    public void disableAutomatedSecurityFixes() throws IOException {
        api().disableAutomatedSecurityFixes(repository.owner(), repository.name());
    }

    /**
     * Tells whether private vulnerability reporting is on ({@code GET
     * /repos/{owner}/{repo}/private-vulnerability-reporting}).
     *
     * @return {@code true} when people can report vulnerabilities privately
     * @throws IOException if the transport fails
     */
    public boolean isPrivateVulnerabilityReportingEnabled() throws IOException {
        return api().checkPrivateVulnerabilityReporting(repository.owner(), repository.name())
                .enabled();
    }

    /**
     * Turns private vulnerability reporting on ({@code PUT /repos/{owner}/{repo}/private-vulnerability-reporting}).
     *
     * @throws IOException if the transport fails
     */
    public void enablePrivateVulnerabilityReporting() throws IOException {
        api().enablePrivateVulnerabilityReporting(repository.owner(), repository.name());
    }

    /**
     * Turns private vulnerability reporting off ({@code DELETE /repos/{owner}/{repo}/private-vulnerability-reporting}).
     *
     * @throws IOException if the transport fails
     */
    public void disablePrivateVulnerabilityReporting() throws IOException {
        api().disablePrivateVulnerabilityReporting(repository.owner(), repository.name());
    }

    /**
     * Tells whether releases are immutable ({@code GET /repos/{owner}/{repo}/immutable-releases}), which means a
     * published release and its assets can no longer be changed.
     *
     * @return {@code true} when immutable releases are enabled
     * @throws IOException if the transport fails
     */
    public boolean isImmutableReleasesEnabled() throws IOException {
        return api().checkImmutableReleases(repository.owner(), repository.name())
                .enabled();
    }

    /**
     * Makes releases immutable ({@code PUT /repos/{owner}/{repo}/immutable-releases}).
     *
     * @throws IOException if the transport fails
     */
    public void enableImmutableReleases() throws IOException {
        api().enableImmutableReleases(repository.owner(), repository.name());
    }

    /**
     * Makes releases mutable again ({@code DELETE /repos/{owner}/{repo}/immutable-releases}).
     *
     * @throws IOException if the transport fails
     */
    public void disableImmutableReleases() throws IOException {
        api().disableImmutableReleases(repository.owner(), repository.name());
    }

    private GitHubRepositoryApi api() {
        return Internals.github(repository).repositories().repository();
    }
}
