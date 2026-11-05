package io.github.piotr_janczukowicz.github.sdk.deploykeys;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKey;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * A deploy key of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response for every field the accessors here do not cover.
 */
public final class DeployKey {

    private final Repository repository;
    private final GitHubDeployKey response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public DeployKey(final Repository repository, final GitHubDeployKey response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the key belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the key id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the title of the key.
     *
     * @return the title
     */
    public String title() {
        return response.title();
    }

    /**
     * Returns the public key.
     *
     * @return the key in OpenSSH format
     */
    public String key() {
        return response.key();
    }

    /**
     * Tells whether the key can only read the repository.
     *
     * @return {@code true} when the key has no write access
     */
    public boolean isReadOnly() {
        return response.readOnly();
    }

    /**
     * Tells whether GitHub verified the key.
     *
     * @return {@code true} when the key is verified
     */
    public boolean isVerified() {
        return response.verified();
    }

    /**
     * Loads the key again ({@code GET /repos/{owner}/{repo}/keys/{key_id}}). This object is not changed.
     *
     * @return the key as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the key is gone
     */
    public DeployKey refresh() throws IOException {
        return repository
                .deployKey(id())
                .orElseThrow(() -> new IllegalStateException("Deploy key " + id() + " is gone"));
    }

    /**
     * Deletes the key ({@code DELETE /repos/{owner}/{repo}/keys/{key_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.deployKeys().deleteDeployKey(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubDeployKey response() {
        return response;
    }
}
