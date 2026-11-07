package io.github.piotr_janczukowicz.github.sdk.advisories;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreatePrivateVulnerabilityReportPayload;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreateRepositoryAdvisoryPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The security advisories of a {@link Repository}. Nothing is requested until a method is called. Reading needs the
 * Repository security advisories permission, and GitHub lists only the published advisories to callers without write
 * access.
 */
public final class RepositoryAdvisories {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryAdvisories(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Lists the advisories ({@code GET /repos/{owner}/{repo}/security-advisories}), newest first. The stream is lazy
     * and can be used once.
     *
     * @return the advisories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Advisory> all() throws IOException {
        return github()
                .securityAdvisories()
                .repositoryAdvisories()
                .listRepositoryAdvisories(repository.owner(), repository.name())
                .stream()
                .map(listed -> new Advisory(repository, listed));
    }

    /**
     * Loads one advisory ({@code GET /repos/{owner}/{repo}/security-advisories/{ghsa_id}}).
     *
     * @param ghsaId the GitHub Security Advisory id
     * @return the advisory, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code ghsaId} is {@code null}
     * @throws IllegalArgumentException if {@code ghsaId} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Advisory> find(final String ghsaId) throws IOException {
        return github().securityAdvisories()
                .repositoryAdvisories()
                .getRepositoryAdvisory(repository.owner(), repository.name(), requireNotBlank(ghsaId, "ghsaId"))
                .map(loaded -> new Advisory(repository, loaded));
    }

    /**
     * Creates a draft advisory ({@code POST /repos/{owner}/{repo}/security-advisories}).
     *
     * @param payload the advisory, as the client record
     * @return the new advisory as GitHub holds it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Advisory create(final GitHubCreateRepositoryAdvisoryPayload payload) throws IOException {
        return new Advisory(
                repository,
                github().securityAdvisories()
                        .repositoryAdvisories()
                        .createRepositoryAdvisory(
                                repository.owner(), repository.name(), requireNonNull(payload, "payload")));
    }

    /**
     * Reports a vulnerability privately to the maintainers ({@code POST
     * /repos/{owner}/{repo}/security-advisories/reports}). The repository must have private vulnerability reporting
     * switched on.
     *
     * @param payload the report, as the client record
     * @return the new advisory as GitHub holds it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Advisory report(final GitHubCreatePrivateVulnerabilityReportPayload payload) throws IOException {
        return new Advisory(
                repository,
                github().securityAdvisories()
                        .repositoryAdvisories()
                        .createPrivateVulnerabilityReport(
                                repository.owner(), repository.name(), requireNonNull(payload, "payload")));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
