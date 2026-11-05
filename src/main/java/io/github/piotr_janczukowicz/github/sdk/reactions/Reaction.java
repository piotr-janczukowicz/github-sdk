package io.github.piotr_janczukowicz.github.sdk.reactions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.IssueComment;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestReviewComment;
import java.io.IOException;
import java.util.Optional;

/**
 * A reaction to an {@link Issue}, an {@link IssueComment}, a {@link PullRequestReviewComment}, a commit comment or a
 * release, as GitHub returned it. The object is immutable, and {@link #response()} gives the complete response for
 * every field the accessors here do not cover. Unlike the other objects it does not navigate to a parent, because the
 * same type serves several kinds of parent.
 */
public final class Reaction {

    private final GitHubReaction response;
    private final ReactionDeletion deletion;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param response the response record the object is built from
     * @param deletion deletes the reaction from the object it belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public Reaction(final GitHubReaction response, final ReactionDeletion deletion) {
        this.response = requireNonNull(response, "response");
        this.deletion = requireNonNull(deletion, "deletion");
    }

    /**
     * Returns the reaction id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the emoji of the reaction.
     *
     * @return the content
     */
    public GitHubReactionContent content() {
        return response.content();
    }

    /**
     * Returns the user who reacted.
     *
     * @return the user, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> user() {
        return Optional.ofNullable(response.user());
    }

    /**
     * Deletes the reaction from the issue or comment it belongs to ({@code DELETE .../reactions/{reaction_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        deletion.delete(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubReaction response() {
        return response;
    }
}
