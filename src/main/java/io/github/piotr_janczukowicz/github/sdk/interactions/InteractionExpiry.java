package io.github.piotr_janczukowicz.github.sdk.interactions;

import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionExpiry;

/** How long an interaction limit lasts. */
public enum InteractionExpiry {
    /** One day, which is GitHub's default. */
    ONE_DAY(GitHubInteractionExpiry.ONE_DAY),
    /** Three days. */
    THREE_DAYS(GitHubInteractionExpiry.THREE_DAYS),
    /** One week. */
    ONE_WEEK(GitHubInteractionExpiry.ONE_WEEK),
    /** One month. */
    ONE_MONTH(GitHubInteractionExpiry.ONE_MONTH),
    /** Six months. */
    SIX_MONTHS(GitHubInteractionExpiry.SIX_MONTHS);

    private final GitHubInteractionExpiry client;

    InteractionExpiry(final GitHubInteractionExpiry client) {
        this.client = client;
    }

    GitHubInteractionExpiry toClient() {
        return client;
    }
}
