package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleasePayload;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link Release} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class ReleaseUpdate {

    private final Release release;
    private @Nullable String tagName;
    private @Nullable String name;
    private @Nullable String body;
    private @Nullable Boolean draft;
    private @Nullable Boolean prerelease;
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param release the release
     * @throws NullPointerException if {@code release} is {@code null}
     */
    public ReleaseUpdate(final Release release) {
        this.release = requireNonNull(release, "release");
    }

    /**
     * Sets the new tag name.
     *
     * @param tagName the new value
     * @return this update
     * @throws NullPointerException if {@code tagName} is {@code null}
     */
    public ReleaseUpdate tagName(final String tagName) {
        this.tagName = requireNonNull(tagName, "tagName");
        return changed();
    }

    /**
     * Sets the new release name.
     *
     * @param name the new value
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public ReleaseUpdate name(final String name) {
        this.name = requireNonNull(name, "name");
        return changed();
    }

    /**
     * Sets the new release notes in Markdown.
     *
     * @param body the new value
     * @return this update
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public ReleaseUpdate body(final String body) {
        this.body = requireNonNull(body, "body");
        return changed();
    }

    /**
     * Sets whether the release is a draft.
     *
     * @param draft the new value
     * @return this update
     */
    public ReleaseUpdate draft(final boolean draft) {
        this.draft = draft;
        return changed();
    }

    /**
     * Sets whether the release is a pre-release.
     *
     * @param prerelease the new value
     * @return this update
     */
    public ReleaseUpdate prerelease(final boolean prerelease) {
        this.prerelease = prerelease;
        return changed();
    }

    /**
     * Sends the changes. The object this update was made from is not changed.
     *
     * @return the release as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Release send() throws IOException {
        state.requireChange();
        return release.apply(new GitHubUpdateReleasePayload(tagName, null, name, body, draft, prerelease, null, null));
    }

    private ReleaseUpdate changed() {
        state.mark();
        return this;
    }
}
