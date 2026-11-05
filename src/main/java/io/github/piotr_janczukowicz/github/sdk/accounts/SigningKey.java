package io.github.piotr_janczukowicz.github.sdk.accounts;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubSshSigningKey;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * An SSH signing key of the authenticated account, which verifies signed commits and tags, as GitHub returned it. The
 * object is immutable, and {@link #response()} gives the complete response.
 */
public final class SigningKey {

    private final GitHubSdk sdk;
    private final GitHubSshSigningKey response;

    public SigningKey(final GitHubSdk sdk, final GitHubSshSigningKey response) {
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
     * Loads the key again ({@code GET /user/ssh_signing_keys/{ssh_signing_key_id}}). This object is not changed.
     *
     * @return the key as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the key is gone
     */
    public SigningKey refresh() throws IOException {
        return github().users()
                .sshSigningKeys()
                .getSshSigningKey(id())
                .map(reloaded -> new SigningKey(sdk, reloaded))
                .orElseThrow(() -> new IllegalStateException("Signing key " + id() + " is gone"));
    }

    /**
     * Deletes the key ({@code DELETE /user/ssh_signing_keys/{ssh_signing_key_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().users().sshSigningKeys().deleteSshSigningKey(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubSshSigningKey response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
