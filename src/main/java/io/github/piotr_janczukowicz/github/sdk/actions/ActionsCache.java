package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCache;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.Optional;

/**
 * A GitHub Actions cache entry of a {@link Repository}. The object is immutable, and {@link #response()} gives the
 * complete response. GitHub has no endpoint to load one cache, so there is no {@code refresh()}.
 */
public final class ActionsCache {

    private final RepositoryActionsCache caches;
    private final GitHubActionsCache response;

    ActionsCache(final RepositoryActionsCache caches, final GitHubActionsCache response) {
        this.caches = requireNonNull(caches, "caches");
        this.response = requireNonNull(response, "response");
        requireNonNull(response.id(), "response.id");
    }

    /**
     * Returns the cache id.
     *
     * @return the id
     * @throws NullPointerException if the response has no id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the cache key.
     *
     * @return the key, or empty when the response has none
     */
    public Optional<String> key() {
        return Optional.ofNullable(response.key());
    }

    /**
     * Returns the Git ref the cache was saved for.
     *
     * @return the ref, or empty when the response has none
     */
    public Optional<String> ref() {
        return Optional.ofNullable(response.ref());
    }

    /**
     * Deletes the cache ({@code DELETE /repos/{owner}/{repo}/actions/caches/{cache_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        caches.delete(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubActionsCache response() {
        return response;
    }
}
