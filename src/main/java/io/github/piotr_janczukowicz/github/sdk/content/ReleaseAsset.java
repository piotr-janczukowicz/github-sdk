package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAsset;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAssetState;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleaseAssetPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A file attached to a {@link Release} of a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class ReleaseAsset {

    private final Repository repository;
    private final GitHubReleaseAsset response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ReleaseAsset(final Repository repository, final GitHubReleaseAsset response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the asset belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the asset id, which is unique in the repository.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the file name of the asset.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the label that GitHub shows instead of the file name.
     *
     * @return the label, or empty when the asset has none
     */
    public Optional<String> label() {
        return Optional.ofNullable(response.label());
    }

    /**
     * Returns the upload state, as the client decoded it.
     *
     * @return the state
     */
    public GitHubReleaseAssetState state() {
        return response.state();
    }

    /**
     * Returns the media type of the file.
     *
     * @return the content type
     */
    public String contentType() {
        return response.contentType();
    }

    /**
     * Returns the size of the file.
     *
     * @return the size in bytes
     */
    public long size() {
        return response.size();
    }

    /**
     * Returns how often the file was downloaded.
     *
     * @return the number of downloads
     */
    public long downloadCount() {
        return response.downloadCount();
    }

    /**
     * Returns the public link to the file. It works without authentication for a public repository; use
     * {@link #download(Path)} for a private one.
     *
     * @return the download URL
     */
    public String downloadUrl() {
        return response.browserDownloadUrl();
    }

    /**
     * Returns the digest of the file, for example {@code sha256:...}.
     *
     * @return the digest, or empty when GitHub has none
     */
    public Optional<String> digest() {
        return Optional.ofNullable(response.digest());
    }

    /**
     * Loads the asset again ({@code GET /repos/{owner}/{repo}/releases/assets/{asset_id}}). This object is not changed.
     *
     * @return the asset as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the asset is gone
     */
    public ReleaseAsset refresh() throws IOException {
        return repository
                .releaseAsset(id())
                .orElseThrow(() -> new IllegalStateException("Release asset " + id() + " is gone"));
    }

    /**
     * Starts a change of the name or label; nothing is sent until {@link ReleaseAssetUpdate#send()}.
     *
     * @return a new update for this asset
     */
    public ReleaseAssetUpdate update() {
        return new ReleaseAssetUpdate(this);
    }

    /**
     * Downloads the file into {@code target} ({@code GET /repos/{owner}/{repo}/releases/assets/{asset_id}} for the
     * binary). The client follows the redirect to the storage host without the {@code Authorization} header and moves
     * the file onto {@code target} only when it is complete.
     *
     * @param target the destination file; its directory must exist and an existing file is replaced
     * @return {@code target}
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or the file cannot be written
     */
    public Path download(final Path target) throws IOException {
        return github().releases()
                .assets()
                .downloadReleaseAsset(repository.owner(), repository.name(), id(), requireNonNull(target, "target"));
    }

    /**
     * Downloads the file into a stream. The stream is not closed.
     *
     * @param out the destination
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long download(final OutputStream out) throws IOException {
        return github().releases()
                .assets()
                .downloadReleaseAsset(repository.owner(), repository.name(), id(), requireNonNull(out, "out"));
    }

    /**
     * Deletes the asset ({@code DELETE /repos/{owner}/{repo}/releases/assets/{asset_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().releases().assets().deleteReleaseAsset(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubReleaseAsset response() {
        return response;
    }

    ReleaseAsset apply(final GitHubUpdateReleaseAssetPayload payload) throws IOException {
        return new ReleaseAsset(
                repository,
                github().releases().assets().updateReleaseAsset(repository.owner(), repository.name(), id(), payload));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
