package io.github.piotr_janczukowicz.github.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueComment;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A comment on an {@link Issue}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class IssueComment {

    private final Issue issue;
    private final GitHubIssueComment response;

    public IssueComment(final Issue issue, final GitHubIssueComment response) {
        this.issue = requireNonNull(issue, "issue");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the issue the comment belongs to.
     *
     * @return the issue
     */
    public Issue issue() {
        return issue;
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
     * @return the body, or empty when GitHub does not return one
     */
    public Optional<String> body() {
        return Optional.ofNullable(response.body());
    }

    /**
     * Returns the user who wrote the comment.
     *
     * @return the author, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(response.user());
    }

    /**
     * Changes the comment text ({@code PATCH /repos/{owner}/{repo}/issues/comments/{comment_id}}). This object is not
     * changed.
     *
     * @param body the new comment text in Markdown
     * @return the comment as GitHub returned it after the change
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IOException if the transport fails
     */
    public IssueComment edit(final String body) throws IOException {
        final GitHubUpdateCommentPayload payload = new GitHubUpdateCommentPayload(requireNonNull(body, "body"));
        final Repository repository = issue.repository();
        final GitHub github = Internals.github(repository);
        return new IssueComment(
                issue, github.issues().comments().updateComment(repository.owner(), repository.name(), id(), payload));
    }

    /**
     * Deletes the comment ({@code DELETE /repos/{owner}/{repo}/issues/comments/{comment_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final Repository repository = issue.repository();
        final GitHub github = Internals.github(repository);
        github.issues().comments().deleteComment(repository.owner(), repository.name(), id());
    }

    /**
     * Loads the comment again ({@code GET /repos/{owner}/{repo}/issues/comments/{comment_id}}). This object is not
     * changed.
     *
     * @return the comment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the comment is gone
     */
    public IssueComment refresh() throws IOException {
        final Repository repository = issue.repository();
        return Internals.github(repository)
                .issues()
                .comments()
                .getComment(repository.owner(), repository.name(), id())
                .map(reloaded -> new IssueComment(issue, reloaded))
                .orElseThrow(() -> new IllegalStateException("Comment " + id() + " is gone"));
    }

    /**
     * Reacts to the comment ({@code POST /repos/{owner}/{repo}/issues/comments/{comment_id}/reactions}). If the current
     * user already reacted with the same emoji, GitHub returns that reaction. This object is not changed.
     *
     * @param content the emoji
     * @return the reaction
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IOException if the transport fails
     */
    public Reaction react(final ReactionContent content) throws IOException {
        final GitHubCreateForCommitCommentPayload payload = new GitHubCreateForCommitCommentPayload(
                ClientValues.reactionToClient(requireNonNull(content, "content")));
        final Repository repository = issue.repository();
        return reaction(Internals.github(repository)
                .reactions()
                .createForIssueComment(repository.owner(), repository.name(), id(), payload));
    }

    /**
     * Lists the reactions to the comment ({@code GET /repos/{owner}/{repo}/issues/comments/{comment_id}/reactions}).
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        final Repository repository = issue.repository();
        return Internals.github(repository)
                .reactions()
                .listForIssueComment(repository.owner(), repository.name(), id())
                .stream()
                .map(this::reaction);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubIssueComment response() {
        return response;
    }

    private Reaction reaction(final GitHubReaction listed) {
        final Repository repository = issue.repository();
        return new Reaction(
                listed,
                reactionId -> Internals.github(repository)
                        .reactions()
                        .deleteForIssueComment(repository.owner(), repository.name(), id(), reactionId));
    }
}
