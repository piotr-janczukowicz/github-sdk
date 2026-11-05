package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubDismissReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReview;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubSubmitReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdateReviewPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A review of a {@link PullRequest}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class PullRequestReview {

    private static final String APPROVED = "APPROVED";
    private static final String DISMISSED = "DISMISSED";
    private static final String PENDING = "PENDING";

    private final PullRequest pullRequest;
    private final GitHubPullRequestReview response;

    PullRequestReview(final PullRequest pullRequest, final GitHubPullRequestReview response) {
        this.pullRequest = requireNonNull(pullRequest, "pullRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the pull request the review belongs to.
     *
     * @return the pull request
     */
    public PullRequest pullRequest() {
        return pullRequest;
    }

    /**
     * Returns the review id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the state exactly as GitHub reports it, for example {@code APPROVED}, {@code CHANGES_REQUESTED},
     * {@code COMMENTED}, {@code DISMISSED} or {@code PENDING}. GitHub's specification does not list the values, so the
     * SDK does not turn them into an enum.
     *
     * @return the state
     */
    public String state() {
        return response.state();
    }

    /**
     * Tells whether the review approves the pull request.
     *
     * @return {@code true} when the state is {@code APPROVED}
     */
    public boolean isApproved() {
        return APPROVED.equals(response.state());
    }

    /**
     * Tells whether the review was dismissed.
     *
     * @return {@code true} when the state is {@code DISMISSED}
     */
    public boolean isDismissed() {
        return DISMISSED.equals(response.state());
    }

    /**
     * Tells whether the review is still a draft that only its author can see.
     *
     * @return {@code true} when the state is {@code PENDING}
     */
    public boolean isPending() {
        return PENDING.equals(response.state());
    }

    /**
     * Returns the review text in Markdown.
     *
     * @return the body, empty when the review has no text
     */
    public String body() {
        return response.body();
    }

    /**
     * Returns the user who wrote the review.
     *
     * @return the author, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(response.user());
    }

    /**
     * Changes the text of the review ({@code PUT /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}}). This
     * object is not changed.
     *
     * @param body the new review text in Markdown
     * @return the review as GitHub returned it after the change
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public PullRequestReview edit(final String body) throws IOException {
        final GitHubUpdateReviewPayload payload = new GitHubUpdateReviewPayload(requireNonNull(body, "body"));
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return new PullRequestReview(
                pullRequest,
                github.pullRequests()
                        .reviews()
                        .update(repository.owner(), repository.name(), pullRequest.number(), id(), payload));
    }

    /**
     * Submits a pending review with no text of its own ({@code POST
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}/events}). This object is not changed.
     *
     * @param event what the review says
     * @return the review as GitHub returned it after the submission
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequestReview submit(final ReviewEvent event) throws IOException {
        return submit(event, null);
    }

    /**
     * Submits a pending review ({@code POST /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}/events}).
     * This object is not changed.
     *
     * @param event what the review says
     * @param body the review text in Markdown, or {@code null} to keep the text the review already has
     * @return the review as GitHub returned it after the submission
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequestReview submit(final ReviewEvent event, final @Nullable String body) throws IOException {
        final GitHubSubmitReviewPayload payload = new GitHubSubmitReviewPayload(
                body, requireNonNull(event, "event").toClient());
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return new PullRequestReview(
                pullRequest,
                github.pullRequests()
                        .reviews()
                        .submit(repository.owner(), repository.name(), pullRequest.number(), id(), payload));
    }

    /**
     * Dismisses the review ({@code PUT /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}/dismissals}). This
     * object is not changed.
     *
     * @param message why the review is dismissed
     * @return the review as GitHub returned it after the dismissal
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IOException if the transport fails
     */
    public PullRequestReview dismiss(final String message) throws IOException {
        final GitHubDismissReviewPayload payload =
                new GitHubDismissReviewPayload(requireNonNull(message, "message"), null);
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return new PullRequestReview(
                pullRequest,
                github.pullRequests()
                        .reviews()
                        .dismiss(repository.owner(), repository.name(), pullRequest.number(), id(), payload));
    }

    /**
     * Deletes the review while it is still pending ({@code DELETE
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void deletePending() throws IOException {
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        github.pullRequests()
                .reviews()
                .deletePending(repository.owner(), repository.name(), pullRequest.number(), id());
    }

    /**
     * Lists the inline comments of the review ({@code GET
     * /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}/comments}). GitHub lists only a summary of each
     * comment, so every comment is loaded again as the stream reaches it, which costs one request per comment. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the comments, oldest first
     * @throws IOException if the first page cannot be loaded
     * @throws IllegalStateException while streaming, if a listed comment is gone
     */
    public Stream<PullRequestReviewComment> comments() throws IOException {
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return github
                .pullRequests()
                .reviews()
                .listComments(repository.owner(), repository.name(), pullRequest.number(), id())
                .stream()
                .map(this::loadComment);
    }

    /**
     * Loads the review again ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}}). This object
     * is not changed.
     *
     * @return the review as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the review is gone
     */
    public PullRequestReview refresh() throws IOException {
        final Repository repository = pullRequest.repository();
        return repository
                .sdk()
                .github()
                .pullRequests()
                .reviews()
                .get(repository.owner(), repository.name(), pullRequest.number(), id())
                .map(reloaded -> new PullRequestReview(pullRequest, reloaded))
                .orElseThrow(() -> new IllegalStateException("Review " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPullRequestReview response() {
        return response;
    }

    private PullRequestReviewComment loadComment(final GitHubReviewComment listed) {
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return Lookups.require(
                () -> github.pullRequests()
                        .comments()
                        .get(repository.owner(), repository.name(), listed.id())
                        .map(loaded -> new PullRequestReviewComment(pullRequest, loaded)),
                () -> "Review comment " + listed.id() + " is gone");
    }
}
