package io.github.piotr_janczukowicz.github.sdk.dependencies;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotPayload;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotResponse;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubDependencyGraphDiffEntry;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubDependencyGraphSpdxSbom;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * The dependency graph of a {@link Repository}: the software bill of materials, the dependency changes between two
 * refs, and the submission of dependencies from a build. The records are returned as GitHub sends them. Nothing is
 * requested until a method is called.
 */
public final class RepositoryDependencies {

    private final Repository repository;

    public RepositoryDependencies(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Exports the software bill of materials in SPDX format ({@code GET /repos/{owner}/{repo}/dependency-graph/sbom}).
     *
     * @return the SBOM as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubDependencyGraphSpdxSbom sbom() throws IOException {
        return github().dependencyGraph().sboms().exportSbom(repository.owner(), repository.name());
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
