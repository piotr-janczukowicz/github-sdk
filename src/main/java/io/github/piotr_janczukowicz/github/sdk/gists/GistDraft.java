package io.github.piotr_janczukowicz.github.sdk.gists;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.gists.GitHubCreateGistFile;
import io.github.piotr_janczukowicz.github.api.gists.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Collects the description and files of a new {@link Gist} and creates it in one request. Nothing is sent until
 * {@link #send()}. The gist is secret unless {@link #listed(boolean)} says otherwise.
 */
public final class GistDraft {

    private final GitHubSdk sdk;
    private final Map<String, GitHubCreateGistFile> files = new LinkedHashMap<>();
    private @Nullable String description;
    private boolean isListed;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @throws NullPointerException if an argument is {@code null}
     */
    public GistDraft(final GitHubSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this draft
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GistDraft description(final String description) {
        this.description = requireNonNull(description, "description");
        return this;
    }

    /**
     * Adds a file. A file added again under the same name replaces the earlier one.
     *
     * @param name the file name
     * @param content the file content, which GitHub does not accept empty
     * @return this draft
     * @throws NullPointerException if {@code name} or {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GistDraft file(final String name, final String content) {
        files.put(requireNotBlank(name, "name"), new GitHubCreateGistFile(requireNonNull(content, "content")));
        return this;
    }

    /**
     * Sets whether the gist is listed publicly.
     *
     * @param isListed {@code true} for a public gist, {@code false} for a secret one
     * @return this draft
     */
    public GistDraft listed(final boolean isListed) {
        this.isListed = isListed;
        return this;
    }

    /**
     * Creates the gist ({@code POST /gists}).
     *
     * @return the new gist
     * @throws IllegalStateException if no file was added
     * @throws IOException if the transport fails
     */
    public Gist send() throws IOException {
        if (files.isEmpty()) {
            throw new IllegalStateException("A gist needs at least one file");
        }
        return new Gist(
                sdk,
                Internals.github(sdk).gists().gist().createGist(new GitHubCreatePayload(description, files, isListed)));
    }
}
