package io.github.piotr_janczukowicz.github.sdk.interactions;

import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionGroup;

/** The group of users who may still comment, open issues and open pull requests while a limit is active. */
public enum InteractionGroup {
    /** Users whose accounts are at least 24 hours old and who have contributed before. */
    EXISTING_USERS(GitHubInteractionGroup.EXISTING_USERS),
    /** Users who have contributed to the repository before. */
    CONTRIBUTORS_ONLY(GitHubInteractionGroup.CONTRIBUTORS_ONLY),
    /** Collaborators only. */
    COLLABORATORS_ONLY(GitHubInteractionGroup.COLLABORATORS_ONLY);

    private final GitHubInteractionGroup client;

    InteractionGroup(final GitHubInteractionGroup client) {
        this.client = client;
    }

    GitHubInteractionGroup toClient() {
        return client;
    }
}
