package io.github.piotr_janczukowicz.github.sdk.reactions;

import java.io.IOException;

/** Deletes a reaction from the object it belongs to. Implemented by the object that hands out the reaction. */
@FunctionalInterface
public interface ReactionDeletion {

    /**
     * Deletes the reaction.
     *
     * @param reactionId the id of the reaction
     * @throws IOException if the transport fails
     */
    void delete(long reactionId) throws IOException;
}
