package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheRetentionLimitForRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheStorageLimitForRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheUsageByRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubDeleteActionsCacheByKeyFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubGetActionsCacheListFilter;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * The GitHub Actions caches of a {@link Repository}, with their usage and limits. Nothing is requested until a method
 * is called. Changing the limits needs the Administration permission, and an organization or enterprise policy can cap
 * what a repository may set.
 */
public final class RepositoryActionsCache {

    private static final int MINIMUM_LIMIT = 1;

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryActionsCache(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Lists the caches ({@code GET /repos/{owner}/{repo}/actions/caches}). The stream is lazy and can be used once.
     *
     * @return the caches
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ActionsCache> caches() throws IOException {
        return github().actions().cache().listActionsCaches(repository.owner(), repository.name()).stream()
                .map(listed -> new ActionsCache(this, listed));
    }

    /**
     * Lists the caches that match a filter, such as a key prefix or a ref, sorted as the filter says.
     *
     * @param filter the client filter record
     * @return the matching caches
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ActionsCache> caches(final GitHubGetActionsCacheListFilter filter) throws IOException {
        return github()
                .actions()
                .cache()
                .listActionsCaches(
                        repository.owner(),
                        repository.name(),
                        requireNonNull(filter, "filter"),
                        GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new ActionsCache(this, listed));
    }

    /**
     * Deletes all caches with a key ({@code DELETE /repos/{owner}/{repo}/actions/caches?key=...}).
     *
     * @param key the exact cache key
     * @return the caches that were deleted
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public List<ActionsCache> deleteByKey(final String key) throws IOException {
        return github()
                .actions()
                .cache()
                .deleteActionsCacheByKey(
                        repository.owner(),
                        repository.name(),
                        GitHubDeleteActionsCacheByKeyFilter.of(requireNotBlank(key, "key")))
                .actionsCaches()
                .stream()
                .map(deleted -> new ActionsCache(this, deleted))
                .toList();
    }

    /**
     * Returns how much cache storage the repository uses ({@code GET /repos/{owner}/{repo}/actions/cache/usage}).
     *
     * @return the usage as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubActionsCacheUsageByRepository usage() throws IOException {
        return github().actions().cache().getRepoCacheUsage(repository.owner(), repository.name());
    }

    /**
     * Returns the number of days a cache is kept without being used ({@code GET
     * /repos/{owner}/{repo}/actions/cache/retention-limit}).
     *
     * @return the days, or empty when GitHub reports none
     * @throws IOException if the transport fails
     */
    public OptionalInt retentionDays() throws IOException {
        final Integer days = github().actions()
                .cache()
                .getRepoCacheRetentionLimit(repository.owner(), repository.name())
                .maxCacheRetentionDays();
        return days == null ? OptionalInt.empty() : OptionalInt.of(days);
    }

    /**
     * Sets the number of days a cache is kept without being used ({@code PUT
     * /repos/{owner}/{repo}/actions/cache/retention-limit}).
     *
     * @param days the days, at least 1
     * @throws IllegalArgumentException if {@code days} is less than 1
     * @throws IOException if the transport fails
     */
    public void setRetentionDays(final int days) throws IOException {
        if (days < MINIMUM_LIMIT) {
            throw new IllegalArgumentException("days must be at least 1: " + days);
        }
        github().actions()
                .cache()
                .setRepoCacheRetentionLimit(
                        repository.owner(), repository.name(), new GitHubActionsCacheRetentionLimitForRepository(days));
    }

    /**
     * Returns the maximum total size of the caches in gigabytes ({@code GET
     * /repos/{owner}/{repo}/actions/cache/storage-limit}).
     *
     * @return the gigabytes, or empty when GitHub reports none
     * @throws IOException if the transport fails
     */
    public OptionalInt storageGigabytes() throws IOException {
        final Integer gigabytes = github().actions()
                .cache()
                .getRepoCacheStorageLimit(repository.owner(), repository.name())
                .maxCacheSizeGb();
        return gigabytes == null ? OptionalInt.empty() : OptionalInt.of(gigabytes);
    }

    /**
     * Sets the maximum total size of the caches in gigabytes ({@code PUT
     * /repos/{owner}/{repo}/actions/cache/storage-limit}).
     *
     * @param gigabytes the gigabytes, at least 1
     * @throws IllegalArgumentException if {@code gigabytes} is less than 1
     * @throws IOException if the transport fails
     */
    public void setStorageGigabytes(final int gigabytes) throws IOException {
        if (gigabytes < MINIMUM_LIMIT) {
            throw new IllegalArgumentException("gigabytes must be at least 1: " + gigabytes);
        }
        github().actions()
                .cache()
                .setRepoCacheStorageLimit(
                        repository.owner(),
                        repository.name(),
                        new GitHubActionsCacheStorageLimitForRepository(gigabytes));
    }

    void delete(final long id) throws IOException {
        github().actions().cache().deleteActionsCacheById(repository.owner(), repository.name(), id);
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
