package io.github.piotr_janczukowicz.github.sdk;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryVisibility;

/** Who can see a {@link Repository}. */
public enum RepositoryVisibility {
    /** Everyone can see the repository. */
    PUBLIC,
    /** Only people with access can see the repository. */
    PRIVATE,
    /** Members of the enterprise can see the repository. Only repositories of an enterprise have it. */
    INTERNAL;

    GitHubRepositoryVisibility toClient() {
        return switch (this) {
            case PUBLIC -> GitHubRepositoryVisibility.PUBLIC;
            case PRIVATE -> GitHubRepositoryVisibility.PRIVATE;
            case INTERNAL -> GitHubRepositoryVisibility.INTERNAL;
        };
    }
}
