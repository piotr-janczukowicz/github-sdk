package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonEmpty;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubMergePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReview;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRemoveReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRequestReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * A pull request of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not
 * cover.
 */
public final class PullRequest {

    private final Repository repository;
    private final GitHubPullRequest response;

    PullRequest(final Repository repository, final GitHubPullRequest response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
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
        return response.number();
    }

    /**
     * Returns the pull request title.
     *
     * @return the title
     */
    public String title() {
        return response.title();
    }

    /**
     * Returns the pull request state. A merged pull request is closed.
     *
     * @return the state
     */
    public PullRequestState state() {
        return PullRequestState.from(response.state());
    }

    /**
     * Returns the user who opened the pull request.
     *
     * @return the author
     */
    public GitHubSimpleUser author() {
        return response.user();
    }

    /**
     * Tells whether the pull request is a draft.
     *
     * @return {@code true} for a draft
     */
    public boolean isDraft() {
        return Boolean.TRUE.equals(response.draft());
    }

    /**
     * Tells whether the pull request was merged, as of the moment it was loaded.
     *
     * @return {@code true} when merged
     */
    public boolean isMerged() {
        return response.merged();
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
     * Starts a change of several fields at once; nothing is sent until {@link PullRequestUpdate#send()}.
     *
     * @return a new update for this pull request
     */
    public PullRequestUpdate update() {
        return new PullRequestUpdate(this);
    }

    /**
     * Closes the pull request ({@code PATCH /repos/{owner}/{repo}/pulls/{pull_number}}). This object is not changed.
     *
     * @return the pull request as GitHub returned it after the change
     * @throws IOException if the transport fails
     */
    public PullRequest close() throws IOException {
        return apply(new GitHubUpdatePullRequestPayload(null, null, GitHubPullRequestUpdateState.CLOSED, null, null));
    }

    /**
     * Reopens the pull request ({@code PATCH /repos/{owner}/{repo}/pulls/{pull_number}}). This object is not changed.
     *
     * @return the pull request as GitHub returned it after the change
     * @throws IOException if the transport fails
     */
    public PullRequest reopen() throws IOException {
        return apply(new GitHubUpdatePullRequestPayload(null, null, GitHubPullRequestUpdateState.OPEN, null, null));
    }

    /**
     * Merges the pull request ({@code PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge}) with GitHub's default
     * commit message and loads it again. This object is not changed.
     *
     * @param method how to merge
     * @return the pull request as GitHub holds it after the merge
     * @throws NullPointerException if {@code method} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the pull request is gone after the merge
     */
    public PullRequest merge(final MergeMethod method) throws IOException {
        return sendMerge(new GitHubMergePullRequestPayload(
                null, null, null, requireNonNull(method, "method").toClient()));
    }

    /**
     * Merges the pull request ({@code PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge}) with a commit message and
     * loads it again. This object is not changed.
     *
     * @param method how to merge
     * @param commitMessage the extra detail for the merge commit
     * @return the pull request as GitHub holds it after the merge
     * @throws NullPointerException if {@code method} or {@code commitMessage} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the pull request is gone after the merge
     */
    public PullRequest merge(final MergeMethod method, final String commitMessage) throws IOException {
        return sendMerge(new GitHubMergePullRequestPayload(
                null,
                requireNonNull(commitMessage, "commitMessage"),
                null,
                requireNonNull(method, "method").toClient()));
    }

    /**
     * Lists the reviews ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}/reviews}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the reviews, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubPullRequestReview> reviews() throws IOException {
        return repository
                .sdk()
                .github()
                .pullRequests()
                .reviews()
                .list(repository.owner(), repository.name(), number())
                .stream();
    }

    /**
     * Creates a review ({@code POST /repos/{owner}/{repo}/pulls/{pull_number}/reviews}).
     *
     * @param payload the review to create
     * @return the review as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubPullRequestReview review(final GitHubCreateReviewPayload payload) throws IOException {
        return repository
                .sdk()
                .github()
                .pullRequests()
                .reviews()
                .create(repository.owner(), repository.name(), number(), requireNonNull(payload, "payload"));
    }

    /**
     * Requests reviews from users ({@code POST /repos/{owner}/{repo}/pulls/{pull_number}/requested_reviewers}) and
     * loads the pull request again. This object is not changed.
     *
     * @param logins the user logins
     * @return the pull request as GitHub holds it after the request
     * @throws IllegalArgumentException if no login is given
     * @throws IOException if the transport fails
     */
    public PullRequest requestReviewers(final String... logins) throws IOException {
        repository
                .sdk()
                .github()
                .pullRequests()
                .reviewRequests()
                .request(
                        repository.owner(),
                        repository.name(),
                        number(),
                        new GitHubRequestReviewersPayload(List.of(requireNonEmpty(logins, "logins")), null));
        return refresh();
    }

    /**
     * Removes requested reviewers ({@code DELETE /repos/{owner}/{repo}/pulls/{pull_number}/requested_reviewers}) and
     * loads the pull request again. This object is not changed.
     *
     * @param logins the user logins
     * @return the pull request as GitHub holds it after the change
     * @throws IllegalArgumentException if no login is given
     * @throws IOException if the transport fails
     */
    public PullRequest removeReviewers(final String... logins) throws IOException {
        repository
                .sdk()
                .github()
                .pullRequests()
                .reviewRequests()
                .remove(
                        repository.owner(),
                        repository.name(),
                        number(),
                        new GitHubRemoveReviewersPayload(List.of(requireNonEmpty(logins, "logins")), null));
        return refresh();
    }

    /**
     * Loads the issue that stands behind this pull request. A pull request is an issue, so its conversation comments
     * are the issue's comments: use {@link Issue#comment(String)} and {@link Issue#comments()} on the result.
     *
     * @return the issue with the same number
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the issue is gone
     */
    public Issue issue() throws IOException {
        return repository
                .issue(number())
                .orElseThrow(() -> new IllegalStateException("Issue " + number() + " of the pull request is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPullRequest response() {
        return response;
    }

    PullRequest apply(final GitHubUpdatePullRequestPayload payload) throws IOException {
        return new PullRequest(
                repository,
                repository
                        .sdk()
                        .github()
                        .pullRequests()
                        .pullRequest()
                        .update(repository.owner(), repository.name(), number(), payload));
    }

    private PullRequest sendMerge(final GitHubMergePullRequestPayload payload) throws IOException {
        repository
                .sdk()
                .github()
                .pullRequests()
                .pullRequest()
                .merge(repository.owner(), repository.name(), number(), payload);
        return refresh();
    }
}
