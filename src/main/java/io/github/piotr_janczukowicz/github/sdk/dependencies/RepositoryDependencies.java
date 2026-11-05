package io.github.piotr_janczukowicz.github.sdk.dependencies;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotPayload;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotResponse;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubDependencyGraphDiffEntry;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubDependencyGraphSpdxSbom;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubGenerateSbomReportResponse;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * The dependency graph of a {@link Repository}: the software bill of materials, the dependency changes between two
 * refs, and the submission of dependencies from a build. The records are returned as GitHub sends them. Nothing is
 * requested until a method is called.
 */
public final class RepositoryDependencies {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryDependencies(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Exports the software bill of materials in SPDX format ({@code GET /repos/{owner}/{repo}/dependency-graph/sbom}).
     *
     * @return the SBOM as GitHub returns it
     * @throws IOException if the transport fails
     * @deprecated GitHub closes this operation down on {@code 2026-11-13}. Use {@link #requestSbomReport()}, then
     *     {@link #sbomReport(String)}.
     */
    @Deprecated(since = "2026-05-12")
    public GitHubDependencyGraphSpdxSbom sbom() throws IOException {
        return github().dependencyGraph().sboms().exportSbom(repository.owner(), repository.name());
    }

    /**
     * Requests the generation of the software bill of materials ({@code GET
     * /repos/{owner}/{repo}/dependency-graph/sbom/generate-report}). This is the first step of the asynchronous flow
     * that replaces {@link #sbom()}; the URL in the result ends with the report id to pass to
     * {@link #sbomReport(String)}. Available on GitHub.com and GitHub Enterprise Cloud only.
     *
     * @return the response with the URL to poll, as GitHub returns it
     * @throws IOException if the transport fails
     * @throws UnsupportedOperationException if the client is limited to another flavor of GitHub
     */
    public GitHubGenerateSbomReportResponse requestSbomReport() throws IOException {
        return github().dependencyGraph().sboms().generateSbomReport(repository.owner(), repository.name());
    }

    /**
     * Looks up a requested software bill of materials ({@code GET
     * /repos/{owner}/{repo}/dependency-graph/sbom/fetch-report/{sbom_uuid}}). Call it again later while the result is
     * empty. The location is a temporary download URL, so treat it as a secret and do not log it. Available on
     * GitHub.com and GitHub Enterprise Cloud only.
     *
     * @param reportId the report id from {@link #requestSbomReport()}
     * @return the temporary download location, or empty while GitHub is still generating the report
     * @throws NullPointerException if {@code reportId} is {@code null}
     * @throws IllegalArgumentException if {@code reportId} is blank
     * @throws IOException if the transport fails
     * @throws UnsupportedOperationException if the client is limited to another flavor of GitHub
     */
    public Optional<URI> sbomReport(final String reportId) throws IOException {
        return github().dependencyGraph()
                .sboms()
                .fetchSbomReport(repository.owner(), repository.name(), requireNotBlank(reportId, "reportId"));
    }

    /**
     * Compares the dependencies of two refs ({@code GET /repos/{owner}/{repo}/dependency-graph/compare/{basehead}}), as
     * a dependency review does.
     *
     * @param base the base ref, a branch, tag or commit
     * @param head the head ref, a branch, tag or commit
     * @return the dependency changes
     * @throws NullPointerException if {@code base} or {@code head} is {@code null}
     * @throws IllegalArgumentException if {@code base} or {@code head} is blank
     * @throws IOException if the transport fails
     */
    public List<GitHubDependencyGraphDiffEntry> diff(final String base, final String head) throws IOException {
        final String basehead = requireNotBlank(base, "base") + "..." + requireNotBlank(head, "head");
        return github().dependencyGraph().dependencyReview().diffRange(repository.owner(), repository.name(), basehead);
    }

    /**
     * Submits a snapshot of the dependencies of a build to the dependency graph ({@code POST
     * /repos/{owner}/{repo}/dependency-graph/snapshots}). The snapshot stays in the dependency graph and cannot be
     * removed through the API, so submit only what should be there.
     *
     * @param snapshot the snapshot, as the client record
     * @return the result as GitHub returns it
     * @throws NullPointerException if {@code snapshot} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubCreateRepositorySnapshotResponse submit(final GitHubCreateRepositorySnapshotPayload snapshot)
            throws IOException {
        return github().dependencyGraph()
                .dependencySubmission()
                .createRepositorySnapshot(repository.owner(), repository.name(), requireNonNull(snapshot, "snapshot"));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
