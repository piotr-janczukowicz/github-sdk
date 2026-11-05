package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleasePayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A release of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Release {

    private final Repository repository;
    private final GitHubRelease response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Release(final Repository repository, final GitHubRelease response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
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
        return response.tagName();
    }

    /**
     * Returns the release name.
     *
     * @return the name, or empty when the release has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Tells whether the release is a draft.
     *
     * @return {@code true} for a draft
     */
    public boolean isDraft() {
        return response.draft();
    }

    /**
     * Tells whether the release is marked as a pre-release.
     *
     * @return {@code true} for a pre-release
     */
    public boolean isPrerelease() {
        return response.prerelease();
    }

    /**
     * Loads the release again ({@code GET /repos/{owner}/{repo}/releases/{release_id}}). This object is not changed.
     *
     * @return the release as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the release is gone
     */
    public Release refresh() throws IOException {
        return github().releases()
                .release()
                .getRelease(repository.owner(), repository.name(), response.id())
                .map(reloaded -> new Release(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Release " + tagName() + " is gone"));
    }

    /**
     * Deletes the release ({@code DELETE /repos/{owner}/{repo}/releases/{release_id}}). The tag stays.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().releases().release().deleteRelease(repository.owner(), repository.name(), response.id());
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
    public GitHubRelease response() {
        return response;
    }

    Release apply(final GitHubUpdateReleasePayload payload) throws IOException {
        return new Release(
                repository,
                github().releases()
                        .release()
                        .updateRelease(repository.owner(), repository.name(), response.id(), payload));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
