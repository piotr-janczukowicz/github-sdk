package io.github.piotr_janczukowicz.github.sdk.accounts;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubKey;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * An SSH authentication key of the authenticated account, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class SshKey {

    private final GitHubSdk sdk;
    private final GitHubKey response;

    public SshKey(final GitHubSdk sdk, final GitHubKey response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
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
     * Tells whether GitHub verified the key.
     *
     * @return {@code true} when the key is verified
     */
    public boolean isVerified() {
        return response.verified();
    }

    /**
     * Tells whether the key can only read.
     *
     * @return {@code true} when the key has no write access
     */
    public boolean isReadOnly() {
        return response.readOnly();
    }

    /**
     * Loads the key again ({@code GET /user/keys/{key_id}}). This object is not changed.
     *
     * @return the key as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the key is gone
     */
    public SshKey refresh() throws IOException {
        return github().users()
                .keys()
                .getPublicKey(id())
                .map(reloaded -> new SshKey(sdk, reloaded))
                .orElseThrow(() -> new IllegalStateException("SSH key " + id() + " is gone"));
    }

    /**
     * Deletes the key ({@code DELETE /user/keys/{key_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().users().keys().deletePublicKey(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubKey response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
