package io.github.piotr_janczukowicz.github.sdk;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;

/** The emoji of a {@link Reaction}. */
public enum ReactionContent {
    /** Thumbs up, {@code +1}. */
    PLUS_ONE,
    /** Thumbs down, {@code -1}. */
    MINUS_ONE,
    /** Laughing face. */
    LAUGH,
    /** Confused face. */
    CONFUSED,
    /** Heart. */
    HEART,
    /** Party popper. */
    HOORAY,
    /** Rocket. */
    ROCKET,
    /** Eyes. */
    EYES;

    static ReactionContent fromClient(final GitHubReactionContent content) {
        return switch (content) {
            case PLUS_ONE -> PLUS_ONE;
            case MINUS_ONE -> MINUS_ONE;
            case LAUGH -> LAUGH;
            case CONFUSED -> CONFUSED;
            case HEART -> HEART;
            case HOORAY -> HOORAY;
            case ROCKET -> ROCKET;
            case EYES -> EYES;
        };
    }

    GitHubReactionContent toClient() {
        return switch (this) {
            case PLUS_ONE -> GitHubReactionContent.PLUS_ONE;
            case MINUS_ONE -> GitHubReactionContent.MINUS_ONE;
            case LAUGH -> GitHubReactionContent.LAUGH;
            case CONFUSED -> GitHubReactionContent.CONFUSED;
            case HEART -> GitHubReactionContent.HEART;
            case HOORAY -> GitHubReactionContent.HOORAY;
            case ROCKET -> GitHubReactionContent.ROCKET;
            case EYES -> GitHubReactionContent.EYES;
        };
    }
}
