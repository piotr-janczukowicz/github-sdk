package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForReleasePayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleasePayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A release of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Release {

    private static final String OCTET_STREAM = "application/octet-stream";

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
     * Lists the files attached to the release ({@code GET /repos/{owner}/{repo}/releases/{release_id}/assets}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the assets
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ReleaseAsset> assets() throws IOException {
        return github()
                .releases()
                .assets()
                .listReleaseAssets(repository.owner(), repository.name(), response.id())
                .stream()
                .map(listed -> new ReleaseAsset(repository, listed));
    }

    /**
     * Attaches a file to the release. The asset is named after the file, and its media type is guessed from the file
     * name, {@code application/octet-stream} when that fails. See {@link #uploadAsset(Path, String, String, String)}.
     *
     * @param file the file to upload
     * @return the new asset as GitHub returned it
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IOException if the file cannot be read or the transport fails
     */
    public ReleaseAsset uploadAsset(final Path file) throws IOException {
        final Path validFile = requireNonNull(file, "file");
        final String contentType =
                Optional.ofNullable(Files.probeContentType(validFile)).orElse(OCTET_STREAM);
        return uploadAsset(validFile, contentType, String.valueOf(validFile.getFileName()), null);
    }

    /**
     * Attaches a file to the release ({@code POST} to the {@code upload_url} of the release, on GitHub's upload host).
     * GitHub rejects a name that the release already has an asset for.
     *
     * @param file the file to upload
     * @param contentType the media type of the file
     * @param name the file name GitHub stores
     * @param label the label GitHub shows instead of the name, or {@code null} for none
     * @return the new asset as GitHub returned it
     * @throws NullPointerException if {@code file}, {@code contentType} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code contentType} or {@code name} is blank
     * @throws IOException if the file cannot be read or the transport fails
     */
    public ReleaseAsset uploadAsset(
            final Path file, final String contentType, final String name, final @Nullable String label)
            throws IOException {
        return new ReleaseAsset(
                repository,
                github().releases()
                        .assets()
                        .uploadReleaseAsset(
                                response.uploadUrl(),
                                requireNonNull(file, "file"),
                                requireNotBlank(contentType, "contentType"),
                                requireNotBlank(name, "name"),
                                label));
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
     * Reacts to the release ({@code POST /repos/{owner}/{repo}/releases/{release_id}/reactions}). A release accepts six
     * emojis: not {@code MINUS_ONE} and not {@code CONFUSED}. A draft release refuses reactions with HTTP 403 (checked
     * live), so publish the release first. If the current user already reacted with the same emoji, GitHub returns that
     * reaction. This object is not changed.
     *
     * @param content the emoji
     * @return the reaction
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code content} is {@code MINUS_ONE} or {@code CONFUSED}
     * @throws IOException if the transport fails
     */
    public Reaction react(final ReactionContent content) throws IOException {
        return reaction(github().reactions()
                .createForRelease(
                        repository.owner(),
                        repository.name(),
                        response.id(),
                        new GitHubCreateForReleasePayload(
                                ClientValues.releaseReactionToClient(requireNonNull(content, "content")))));
    }

    /**
     * Lists the reactions to the release ({@code GET /repos/{owner}/{repo}/releases/{release_id}/reactions}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        return github().reactions().listForRelease(repository.owner(), repository.name(), response.id()).stream()
                .map(this::reaction);
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

    private Reaction reaction(final GitHubReaction listed) {
        return new Reaction(
                listed,
                reactionId -> github().reactions()
                        .deleteForRelease(repository.owner(), repository.name(), response.id(), reactionId));
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
