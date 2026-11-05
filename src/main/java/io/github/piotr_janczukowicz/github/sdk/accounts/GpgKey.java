package io.github.piotr_janczukowicz.github.sdk.accounts;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A GPG key of the authenticated account, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response, including the emails and subkeys, for every field the accessors here do not cover.
 */
public final class GpgKey {

    private final GitHubSdk sdk;
    private final GitHubGpgKey response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GpgKey(final GitHubSdk sdk, final GitHubGpgKey response) {
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
     * Returns the name of the key.
     *
     * @return the name, or empty when the key has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the long id of the key as GPG shows it.
     *
     * @return the key id
     */
    public String keyId() {
        return response.keyId();
    }

    /**
     * Tells whether the key can sign commits and tags.
     *
     * @return {@code true} when the key can sign
     */
    public boolean canSign() {
        return response.canSign();
    }

    /**
     * Loads the key again ({@code GET /user/gpg_keys/{gpg_key_id}}). This object is not changed.
     *
     * @return the key as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the key is gone
     */
    public GpgKey refresh() throws IOException {
        return github().users()
                .gpgKeys()
                .getGpgKey(id())
                .map(reloaded -> new GpgKey(sdk, reloaded))
                .orElseThrow(() -> new IllegalStateException("GPG key " + id() + " is gone"));
    }

    /**
     * Deletes the key ({@code DELETE /user/gpg_keys/{gpg_key_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().users().gpgKeys().deleteGpgKey(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubGpgKey response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
