package io.github.piotr_janczukowicz.github.sdk.pulls;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonEmpty;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewCommentPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubMergePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRemoveReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubRequestReviewersPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A pull request of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not
 * cover.
 */
public final class PullRequest {

    private final Repository repository;
    private final GitHubPullRequest response;

    public PullRequest(final Repository repository, final GitHubPullRequest response) {
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
        return Internals.github(repository)
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
        return Internals.github(repository)
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
    public Stream<PullRequestReview> reviews() throws IOException {
        return Internals.github(repository)
                .pullRequests()
                .reviews()
                .list(repository.owner(), repository.name(), number())
                .stream()
                .map(listed -> new PullRequestReview(this, listed));
    }

    /**
     * Loads one review ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}}).
     *
     * @param id the review id
     * @return the review, or empty when GitHub has none with this id
     * @throws IOException if the transport fails
     */
    public Optional<PullRequestReview> review(final long id) throws IOException {
        return Internals.github(repository)
                .pullRequests()
                .reviews()
                .get(repository.owner(), repository.name(), number(), id)
                .map(loaded -> new PullRequestReview(this, loaded));
    }

    /**
     * Reviews the pull request with no text and submits the review at once ({@code POST
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews}). This object is not changed.
     *
     * @param event what the review says; GitHub requires text for every event but {@link ReviewEvent#APPROVE}, so use
     *     {@link #review(ReviewEvent, String)} for the others
     * @return the submitted review
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequestReview review(final ReviewEvent event) throws IOException {
        return review(event, null);
    }

    /**
     * Reviews the pull request and submits the review at once ({@code POST
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews}). This object is not changed.
     *
     * @param event what the review says
     * @param body the review text in Markdown, or {@code null} for none
     * @return the submitted review
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequestReview review(final ReviewEvent event, final @Nullable String body) throws IOException {
        return createReview(new GitHubCreateReviewPayload(
                null, body, requireNonNull(event, "event").toClient(), null));
    }

    /**
     * Starts a review that stays pending, visible only to its author, until
     * {@link PullRequestReview#submit(ReviewEvent)} is called ({@code POST
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews}). This object is not changed.
     *
     * @param body the review text in Markdown, or {@code null} for none
     * @return the pending review
     * @throws IOException if the transport fails
     */
    public PullRequestReview draftReview(final @Nullable String body) throws IOException {
        return createReview(new GitHubCreateReviewPayload(null, body, null, null));
    }

    /**
     * Lists the inline comments on the diff, from every review ({@code GET
     * /repos/{owner}/{repo}/pulls/{pull_number}/comments}). Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the comments, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequestReviewComment> reviewComments() throws IOException {
        return Internals.github(repository)
                .pullRequests()
                .comments()
                .list(repository.owner(), repository.name(), number())
                .stream()
                .map(listed -> new PullRequestReviewComment(this, listed));
    }

    /**
     * Adds an inline comment on one line of the diff, outside any review ({@code POST
     * /repos/{owner}/{repo}/pulls/{pull_number}/comments}). This object is not changed.
     *
     * @param commitId the sha of the commit to comment on, usually the head of the pull request
     * @param path the file path, relative to the repository root
     * @param line the line in the file to comment on
     * @param body the comment text in Markdown
     * @return the new comment
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code commitId}, {@code path} or {@code body} is blank
     * @throws IOException if the transport fails
     */
    public PullRequestReviewComment commentOnLine(
            final String commitId, final String path, final int line, final String body) throws IOException {
        final GitHubCreateReviewCommentPayload payload = new GitHubCreateReviewCommentPayload(
                requireNonNull(body, "body"),
                requireNonNull(commitId, "commitId"),
                requireNonNull(path, "path"),
                null,
                null,
                line,
                null,
                null,
                null,
                null);
        return new PullRequestReviewComment(
                this,
                Internals.github(repository)
                        .pullRequests()
                        .comments()
                        .create(repository.owner(), repository.name(), number(), payload));
    }

    /**
     * Loads the users and teams whose review is requested ({@code GET
     * /repos/{owner}/{repo}/pulls/{pull_number}/requested_reviewers}). This object is not changed.
     *
     * @return the requested reviewers, as the client returned them
     * @throws IOException if the transport fails
     */
    public GitHubPullRequestReviewRequest reviewRequests() throws IOException {
        return Internals.github(repository)
                .pullRequests()
                .reviewRequests()
                .list(repository.owner(), repository.name(), number());
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
        Internals.github(repository)
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
        Internals.github(repository)
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
                Internals.github(repository)
                        .pullRequests()
                        .pullRequest()
                        .update(repository.owner(), repository.name(), number(), payload));
    }

    private PullRequestReview createReview(final GitHubCreateReviewPayload payload) throws IOException {
        return new PullRequestReview(
                this,
                Internals.github(repository)
                        .pullRequests()
                        .reviews()
                        .create(repository.owner(), repository.name(), number(), payload));
    }

    private PullRequest sendMerge(final GitHubMergePullRequestPayload payload) throws IOException {
        Internals.github(repository)
                .pullRequests()
                .pullRequest()
                .merge(repository.owner(), repository.name(), number(), payload);
        return refresh();
    }
}
