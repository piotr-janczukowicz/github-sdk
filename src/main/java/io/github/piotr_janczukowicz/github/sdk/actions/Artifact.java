package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifact;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.net.URI;

/**
 * A build artifact of a workflow run, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Artifact {

    private final Repository repository;
    private final GitHubArtifact response;

    public Artifact(final Repository repository, final GitHubArtifact response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the artifact belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the artifact id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the name of the artifact.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the size of the artifact archive.
     *
     * @return the size in bytes
     */
    public int sizeInBytes() {
        return response.sizeInBytes();
    }

    /**
     * Tells whether GitHub has already removed the artifact contents because its retention ended.
     *
     * @return {@code true} when the artifact has expired
     */
    public boolean isExpired() {
        return response.expired();
    }

    /**
     * Loads the artifact again ({@code GET /repos/{owner}/{repo}/actions/artifacts/{artifact_id}}). This object is not
     * changed.
     *
     * @return the artifact as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the artifact is gone
     */
    public Artifact refresh() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.actions()
                .artifacts()
                .getArtifact(repository.owner(), repository.name(), id())
                .map(reloaded -> new Artifact(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Artifact " + id() + " is gone"));
    }

    /**
     * Asks for the link to the artifact as a zip archive ({@code GET
     * /repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip}). The link is short-lived, and this method downloads
     * nothing.
     *
     * @return the link GitHub redirects to
     * @throws IOException if the transport fails
     */
    public URI downloadUrl() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.actions().artifacts().downloadArtifact(repository.owner(), repository.name(), id());
    }

    /**
     * Deletes the artifact ({@code DELETE /repos/{owner}/{repo}/actions/artifacts/{artifact_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.actions().artifacts().deleteArtifact(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubArtifact response() {
        return response;
    }
}
