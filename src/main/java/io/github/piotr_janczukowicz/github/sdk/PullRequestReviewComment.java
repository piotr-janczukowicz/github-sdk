package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewCommentReplyPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdateReviewCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * An inline comment on the diff of a {@link PullRequest}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class PullRequestReviewComment {

    private final PullRequest pullRequest;
    private final GitHubPullRequestReviewComment response;

    PullRequestReviewComment(final PullRequest pullRequest, final GitHubPullRequestReviewComment response) {
        this.pullRequest = requireNonNull(pullRequest, "pullRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the pull request the comment belongs to.
     *
     * @return the pull request
     */
    public PullRequest pullRequest() {
        return pullRequest;
    }

    /**
     * Returns the comment id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the comment text in Markdown.
     *
     * @return the body
     */
    public String body() {
        return response.body();
    }

    /**
     * Returns the path of the file the comment is on.
     *
     * @return the path, relative to the repository root
     */
    public String path() {
        return response.path();
    }

    /**
     * Returns the line the comment is on.
     *
     * @return the line in the file, or empty when the comment is on the whole file or the line no longer exists
     */
    public OptionalInt line() {
        final Integer line = response.line();
        return line == null ? OptionalInt.empty() : OptionalInt.of(line);
    }

    /**
     * Returns the user who wrote the comment.
     *
     * @return the author, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(response.user()).map(OptionalNull::value);
    }

    /**
     * Changes the comment text ({@code PATCH /repos/{owner}/{repo}/pulls/comments/{comment_id}}). This object is not
     * changed.
     *
     * @param body the new comment text in Markdown
     * @return the comment as GitHub returned it after the change
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public PullRequestReviewComment edit(final String body) throws IOException {
        final GitHubUpdateReviewCommentPayload payload =
                new GitHubUpdateReviewCommentPayload(requireNonNull(body, "body"));
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return new PullRequestReviewComment(
                pullRequest,
                github.pullRequests().comments().update(repository.owner(), repository.name(), id(), payload));
    }

    /**
     * Replies to this comment ({@code POST /repos/{owner}/{repo}/pulls/{pull_number}/comments/{comment_id}/replies}).
     * This object is not changed.
     *
     * @param body the reply text in Markdown
     * @return the new reply
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public PullRequestReviewComment reply(final String body) throws IOException {
        final GitHubCreateReviewCommentReplyPayload payload =
                new GitHubCreateReviewCommentReplyPayload(requireNonNull(body, "body"));
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        return new PullRequestReviewComment(
                pullRequest,
                github.pullRequests()
                        .comments()
                        .createReply(repository.owner(), repository.name(), pullRequest.number(), id(), payload));
    }

    /**
     * Deletes the comment ({@code DELETE /repos/{owner}/{repo}/pulls/comments/{comment_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final Repository repository = pullRequest.repository();
        final GitHub github = repository.sdk().github();
        github.pullRequests().comments().delete(repository.owner(), repository.name(), id());
    }

    /**
     * Loads the comment again ({@code GET /repos/{owner}/{repo}/pulls/comments/{comment_id}}). This object is not
     * changed.
     *
     * @return the comment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the comment is gone
     */
    public PullRequestReviewComment refresh() throws IOException {
        final Repository repository = pullRequest.repository();
        return repository
                .sdk()
                .github()
                .pullRequests()
                .comments()
                .get(repository.owner(), repository.name(), id())
                .map(reloaded -> new PullRequestReviewComment(pullRequest, reloaded))
                .orElseThrow(() -> new IllegalStateException("Review comment " + id() + " is gone"));
    }

    /**
     * Reacts to the comment ({@code POST /repos/{owner}/{repo}/pulls/comments/{comment_id}/reactions}). If the current
     * user already reacted with the same emoji, GitHub returns that reaction. This object is not changed.
     *
     * @param content the emoji
     * @return the reaction
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IOException if the transport fails
     */
    public Reaction react(final ReactionContent content) throws IOException {
        final GitHubCreateForCommitCommentPayload payload = new GitHubCreateForCommitCommentPayload(
                requireNonNull(content, "content").toClient());
        final Repository repository = pullRequest.repository();
        return reaction(repository
                .sdk()
                .github()
                .reactions()
                .createForPullRequestReviewComment(repository.owner(), repository.name(), id(), payload));
    }

    /**
     * Lists the reactions to the comment ({@code GET /repos/{owner}/{repo}/pulls/comments/{comment_id}/reactions}).
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        final Repository repository = pullRequest.repository();
        return repository
                .sdk()
                .github()
                .reactions()
                .listForPullRequestReviewComment(repository.owner(), repository.name(), id())
                .stream()
                .map(this::reaction);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPullRequestReviewComment response() {
        return response;
    }

    private Reaction reaction(final GitHubReaction listed) {
        final Repository repository = pullRequest.repository();
        return new Reaction(
                listed,
                reactionId -> repository
                        .sdk()
                        .github()
                        .reactions()
                        .deleteForPullRequestComment(repository.owner(), repository.name(), id(), reactionId));
    }
}
