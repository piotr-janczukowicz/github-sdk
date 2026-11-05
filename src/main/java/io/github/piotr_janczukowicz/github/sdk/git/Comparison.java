package io.github.piotr_janczukowicz.github.sdk.git;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparison;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparisonStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The difference between two commits, branches or tags of a {@link Repository}, as GitHub returned it. The object is
 * immutable, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * GitHub embeds at most 250 commits in one comparison, so {@link #commits()} may hold fewer than
 * {@link #totalCommits()}.
 */
public final class Comparison {

    private final Repository repository;
    private final String baseHead;
    private final GitHubCommitComparison response;

    public Comparison(final Repository repository, final String baseHead, final GitHubCommitComparison response) {
        this.repository = requireNonNull(repository, "repository");
        this.baseHead = requireNonNull(baseHead, "baseHead");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository that was compared.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns how the head relates to the base, as the client decoded it. The client maps a value it does not know to
     * its own {@code UNRECOGNIZED} constant.
     *
     * @return the status
     */
    public GitHubCommitComparisonStatus status() {
        return response.status();
    }

    /**
     * Returns the number of commits the head has that the base does not.
     *
     * @return the number of commits
     */
    public int aheadBy() {
        return response.aheadBy();
    }

    /**
     * Returns the number of commits the base has that the head does not.
     *
     * @return the number of commits
     */
    public int behindBy() {
        return response.behindBy();
    }

    /**
     * Returns the number of commits between the base and the head.
     *
     * @return the total number of commits
     */
    public int totalCommits() {
        return response.totalCommits();
    }

    /**
     * Returns the commits that GitHub embedded in the comparison, at most 250.
     *
     * @return the commits, oldest first
     */
    public List<Commit> commits() {
        return response.commits().stream()
                .map(listed -> new Commit(repository, listed))
                .toList();
    }

    /**
     * Returns the changed files that GitHub embedded in the comparison.
     *
     * @return the changed files
     */
    public List<GitHubDiffEntry> files() {
        return response.files();
    }

    /**
     * Loads the comparison as a unified diff ({@code GET /repos/{owner}/{repo}/compare/{basehead}} with the diff media
     * type).
     *
     * @return the diff, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<String> diff() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.commits().commit().compareCommitsDiff(repository.owner(), repository.name(), baseHead);
    }

    /**
     * Loads the comparison as a patch series ({@code GET /repos/{owner}/{repo}/compare/{basehead}} with the patch media
     * type).
     *
     * @return the patch, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<String> patch() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.commits().commit().compareCommitsPatch(repository.owner(), repository.name(), baseHead);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommitComparison response() {
        return response;
    }
}
