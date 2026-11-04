package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;

/**
 * Entry point of the object layer. It wraps an existing {@link GitHub} client, so authentication, flavor, GHES version
 * and limits are configured on the client as usual; the SDK has no configuration of its own.
 */
public final class GitHubSdk {

    private final GitHub github;

    private GitHubSdk(final GitHub github) {
        this.github = requireNonNull(github, "github");
    }

    /**
     * Wraps a client.
     *
     * @param github the client every object of this SDK calls
     * @return the entry point
     * @throws NullPointerException if {@code github} is {@code null}
     */
    public static GitHubSdk of(final GitHub github) {
        return new GitHubSdk(github);
    }

    /**
     * Returns a handle for a repository. It makes no request and holds no data; call {@link Repository#fetch()} to load
     * it.
     *
     * @param owner the account that owns the repository
     * @param name the repository name
     * @return the repository handle
     * @throws NullPointerException if {@code owner} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code owner} or {@code name} is blank
     */
    public Repository repository(final String owner, final String name) {
        return new Repository(this, requireNotBlank(owner, "owner"), requireNotBlank(name, "name"), null);
    }

    /**
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitHub github() {
        return github;
    }
}
