package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.Optional;

/**
 * A reaction to an {@link Issue}, an {@link IssueComment} or a {@link PullRequestReviewComment}, as GitHub returned it.
 * The object is immutable, and {@link #response()} gives the complete response for every field the accessors here do
 * not cover. Unlike the other objects it does not navigate to a parent, because the same type serves three kinds of
 * parent.
 */
public final class Reaction {

    private final GitHubReaction response;
    private final Deletion deletion;

    Reaction(final GitHubReaction response, final Deletion deletion) {
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
    public ReactionContent content() {
        return ReactionContent.fromClient(response.content());
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

    /** Removes a reaction from the parent it was made on. */
    @FunctionalInterface
    interface Deletion {

        /**
         * Removes the reaction.
         *
         * @param reactionId the id of the reaction
         * @throws IOException if the transport fails
         */
        void delete(long reactionId) throws IOException;
    }
}
