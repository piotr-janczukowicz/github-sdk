package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleasePayload;
import java.io.IOException;
import java.util.Optional;

/**
 * A release of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #record()} gives the complete response for every field the accessors here do not cover.
 */
public final class Release {

    private final Repository repository;
    private final GitHubRelease record;

    Release(final Repository repository, final GitHubRelease record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the repository the release belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the name of the tag the release is built on.
     *
     * @return the tag name
     */
    public String tagName() {
        return record.tagName();
    }

    /**
     * Returns the release name.
     *
     * @return the name, or empty when the release has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(record.name());
    }

    /**
     * Tells whether the release is a draft.
     *
     * @return {@code true} for a draft
     */
    public boolean isDraft() {
        return record.draft();
    }

    /**
     * Tells whether the release is marked as a pre-release.
     *
     * @return {@code true} for a pre-release
     */
    public boolean isPrerelease() {
        return record.prerelease();
    }

    /**
     * Loads the release again ({@code GET /repos/{owner}/{repo}/releases/{release_id}}). This object is not changed.
     *
     * @return the release as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the release is gone
     */
    public Release refresh() throws IOException {
        return repository
                .sdk()
                .github()
                .releases()
                .release()
                .getRelease(repository.owner(), repository.name(), record.id())
                .map(reloaded -> new Release(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Release " + tagName() + " is gone"));
    }

    /**
     * Deletes the release ({@code DELETE /repos/{owner}/{repo}/releases/{release_id}}). The tag stays.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        repository
                .sdk()
                .github()
                .releases()
                .release()
                .deleteRelease(repository.owner(), repository.name(), record.id());
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link ReleaseUpdate#send()}.
     *
     * @return a new update for this release
     */
    public ReleaseUpdate update() {
        return new ReleaseUpdate(this);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubRelease record() {
        return record;
    }

    Release apply(final GitHubUpdateReleasePayload payload) throws IOException {
        return new Release(
                repository,
                repository
                        .sdk()
                        .github()
                        .releases()
                        .release()
                        .updateRelease(repository.owner(), repository.name(), record.id(), payload));
    }
}
