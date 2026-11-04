package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * A pull request of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #record()} gives the complete response for every field the accessors here do not
 * cover.
 */
public final class PullRequest {

    private final Repository repository;
    private final GitHubPullRequest record;

    PullRequest(final Repository repository, final GitHubPullRequest record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the repository the pull request belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the pull request number within its repository.
     *
     * @return the number
     */
    public int number() {
        return record.number();
    }

    /**
     * Returns the pull request title.
     *
     * @return the title
     */
    public String title() {
        return record.title();
    }

    /**
     * Returns the pull request state. A merged pull request is closed.
     *
     * @return the state
     */
    public PullRequestState state() {
        return PullRequestState.from(record.state());
    }

    /**
     * Returns the user who opened the pull request.
     *
     * @return the author
     */
    public GitHubSimpleUser author() {
        return record.user();
    }

    /**
     * Tells whether the pull request is a draft.
     *
     * @return {@code true} for a draft
     */
    public boolean isDraft() {
        return Boolean.TRUE.equals(record.draft());
    }

    /**
     * Tells whether the pull request was merged, as of the moment it was loaded.
     *
     * @return {@code true} when merged
     */
    public boolean isMerged() {
        return record.merged();
    }

    /**
     * Loads the pull request again ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}}). This object is not changed.
     *
     * @return the pull request as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the pull request is gone
     */
    public PullRequest refresh() throws IOException {
        return repository
                .pullRequest(number())
                .orElseThrow(() -> new IllegalStateException("Pull request " + number() + " is gone"));
    }

    /**
     * Lists the changed files ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}/files}). Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the changed files
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubDiffEntry> files() throws IOException {
        return repository
                .sdk()
                .github()
                .pullRequests()
                .pullRequest()
                .listFiles(repository.owner(), repository.name(), number())
                .stream();
    }

    /**
     * Lists the commits ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}/commits}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the commits, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCommit> commits() throws IOException {
        return repository
                .sdk()
                .github()
                .pullRequests()
                .pullRequest()
                .listCommits(repository.owner(), repository.name(), number())
                .stream();
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPullRequest record() {
        return record;
    }
}
