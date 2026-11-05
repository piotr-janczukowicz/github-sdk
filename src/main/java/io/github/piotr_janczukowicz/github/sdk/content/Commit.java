package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubListForRefFilter;
import io.github.piotr_janczukowicz.github.api.checks.GitHubListForRefFilterBuilder;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCreateCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCreateCommitStatusPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckSuite;
import io.github.piotr_janczukowicz.github.sdk.checks.CombinedStatus;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatus;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatusState;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
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

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Commit(final Repository repository, final GitHubCommit response) {
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
        return github().checks().runs().listForRef(repository.owner(), repository.name(), sha()).stream()
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
        return github()
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
        return github()
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
        return github().commits()
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
                ClientValues.statusStateToClient(requireNonNull(state, "state")), targetUrl, description, context);
        return new CommitStatus(
                this,
                github().commits()
                        .statuses()
                        .createCommitStatus(repository.owner(), repository.name(), sha(), payload));
    }

    /**
     * Lists the comments on the commit ({@code GET /repos/{owner}/{repo}/commits/{commit_sha}/comments}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the comments, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitComment> comments() throws IOException {
        return github()
                .commits()
                .comments()
                .listCommentsForCommit(repository.owner(), repository.name(), sha(), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new CommitComment(repository, listed));
    }

    /**
     * Comments on the commit as a whole ({@code POST /repos/{owner}/{repo}/commits/{commit_sha}/comments}).
     *
     * @param body the comment text
     * @return the new comment
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public CommitComment comment(final String body) throws IOException {
        return createComment(new GitHubCreateCommitCommentPayload(requireNotBlank(body, "body"), null, null, null));
    }

    /**
     * Comments on a line of a file in the commit ({@code POST /repos/{owner}/{repo}/commits/{commit_sha}/comments}).
     *
     * @param body the comment text
     * @param path the path of the file
     * @param line the line of the file
     * @return the new comment
     * @throws NullPointerException if {@code body} or {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code body} or {@code path} is blank
     * @throws IOException if the transport fails
     */
    public CommitComment comment(final String body, final String path, final int line) throws IOException {
        return createComment(new GitHubCreateCommitCommentPayload(
                requireNotBlank(body, "body"), requireNotBlank(path, "path"), null, line));
    }

    /**
     * Lists the check suites of the commit ({@code GET /repos/{owner}/{repo}/commits/{ref}/check-suites}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the check suites
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CheckSuite> checkSuites() throws IOException {
        return github().checks().suites().listSuitesForRef(repository.owner(), repository.name(), sha()).stream()
                .map(listed -> new CheckSuite(repository, listed));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommit response() {
        return response;
    }

    private CommitComment createComment(final GitHubCreateCommitCommentPayload payload) throws IOException {
        return new CommitComment(
                repository,
                github().commits()
                        .comments()
                        .createCommitComment(repository.owner(), repository.name(), sha(), payload));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
