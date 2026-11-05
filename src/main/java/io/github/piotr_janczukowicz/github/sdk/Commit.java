package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.checks.GitHubListForRefFilter;
import io.github.piotr_janczukowicz.github.api.checks.GitHubListForRefFilterBuilder;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCreateCommitStatusPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A commit of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Commit {

    private final Repository repository;
    private final GitHubCommit response;

    Commit(final Repository repository, final GitHubCommit response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the commit belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the commit hash.
     *
     * @return the SHA
     */
    public String sha() {
        return response.sha();
    }

    /**
     * Returns the commit message.
     *
     * @return the message
     */
    public String message() {
        return response.commit().message();
    }

    /**
     * Returns the GitHub user who wrote the commit.
     *
     * @return the author, or empty when the commit is not linked to a GitHub user
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(response.author());
    }

    /**
     * Lists the check runs of the commit ({@code GET /repos/{owner}/{repo}/commits/{ref}/check-runs}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the check runs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CheckRun> checkRuns() throws IOException {
        return repository
                .sdk()
                .github()
                .checks()
                .runs()
                .listForRef(repository.owner(), repository.name(), sha())
                .stream()
                .map(listed -> new CheckRun(repository, listed));
    }

    /**
     * Lists the check runs of the commit that have one name ({@code GET
     * /repos/{owner}/{repo}/commits/{ref}/check-runs?check_name=...}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param name the name of the check, for example {@code build}
     * @return the check runs with this name
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CheckRun> checkRuns(final String name) throws IOException {
        final GitHubListForRefFilter filter = GitHubListForRefFilterBuilder.builder()
                .checkName(requireNotBlank(name, "name"))
                .build();
        return repository
                .sdk()
                .github()
                .checks()
                .runs()
                .listForRef(repository.owner(), repository.name(), sha(), filter, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new CheckRun(repository, listed));
    }

    /**
     * Lists every status that services reported for the commit ({@code GET
     * /repos/{owner}/{repo}/commits/{ref}/statuses}). Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the statuses, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitStatus> statuses() throws IOException {
        return repository
                .sdk()
                .github()
                .commits()
                .statuses()
                .listCommitStatusesForRef(repository.owner(), repository.name(), sha())
                .stream()
                .map(listed -> new CommitStatus(this, listed));
    }

    /**
     * Loads the combined state of the commit's statuses ({@code GET /repos/{owner}/{repo}/commits/{ref}/status}).
     *
     * @return the combined status, or empty when GitHub does not have the commit
     * @throws IOException if the transport fails
     */
    public Optional<CombinedStatus> combinedStatus() throws IOException {
        return repository
                .sdk()
                .github()
                .commits()
                .statuses()
                .getCombinedStatusForRef(repository.owner(), repository.name(), sha())
                .map(loaded -> new CombinedStatus(this, loaded));
    }

    /**
     * Reports a status for the commit with no description or link ({@code POST /repos/{owner}/{repo}/statuses/{sha}}).
     * This object is not changed.
     *
     * @param state the state to report
     * @param context the name of the service or check that reports, for example {@code ci/build}
     * @return the new status
     * @throws NullPointerException if {@code state} is {@code null}
     * @throws IOException if the transport fails
     */
    public CommitStatus createStatus(final CommitStatusState state, final String context) throws IOException {
        return createStatus(state, context, null, null);
    }

    /**
     * Reports a status for the commit ({@code POST /repos/{owner}/{repo}/statuses/{sha}}). This object is not changed.
     *
     * @param state the state to report
     * @param context the name of the service or check that reports, for example {@code ci/build}
     * @param description a short description, or {@code null} for none
     * @param targetUrl a link to the service's page about the status, or {@code null} for none
     * @return the new status
     * @throws NullPointerException if {@code state} is {@code null}
     * @throws IOException if the transport fails
     */
    public CommitStatus createStatus(
            final CommitStatusState state,
            final String context,
            final @Nullable String description,
            final @Nullable String targetUrl)
            throws IOException {
        final GitHubCreateCommitStatusPayload payload = new GitHubCreateCommitStatusPayload(
                requireNonNull(state, "state").toClient(), targetUrl, description, context);
        return new CommitStatus(
                this,
                repository
                        .sdk()
                        .github()
                        .commits()
                        .statuses()
                        .createCommitStatus(repository.owner(), repository.name(), sha(), payload));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommit response() {
        return response;
    }
}
