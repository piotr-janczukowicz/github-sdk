package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleaseAssetPayload;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link ReleaseAsset} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class ReleaseAssetUpdate {

    private final ReleaseAsset asset;
    private @Nullable String name;
    private @Nullable String label;
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param asset the asset
     * @throws NullPointerException if {@code asset} is {@code null}
     */
    public ReleaseAssetUpdate(final ReleaseAsset asset) {
        this.asset = requireNonNull(asset, "asset");
    }

    /**
     * Sets the new file name.
     *
     * @param name the new value
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ReleaseAssetUpdate name(final String name) {
        this.name = requireNotBlank(name, "name");
        return changed();
    }

    /**
     * Sets the new label.
     *
     * @param label the new value
     * @return this update
     * @throws NullPointerException if {@code label} is {@code null}
     */
    public ReleaseAssetUpdate label(final String label) {
        this.label = requireNonNull(label, "label");
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}/releases/assets/{asset_id}}). The object this update was
     * made from is not changed.
     *
     * @return the asset as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public ReleaseAsset send() throws IOException {
        state.requireChange();
        return asset.apply(new GitHubUpdateReleaseAssetPayload(name, label, null));
    }

    private ReleaseAssetUpdate changed() {
        state.mark();
        return this;
    }
}
