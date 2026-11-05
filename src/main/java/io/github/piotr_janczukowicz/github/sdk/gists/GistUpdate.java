package io.github.piotr_janczukowicz.github.sdk.gists;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.gists.GitHubUpdateGistFile;
import io.github.piotr_janczukowicz.github.api.gists.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link Gist} and sends them in one request. Only what was chosen is sent, and nothing is sent
 * until {@link #send()}. When the same file is chosen more than once, the last choice counts.
 */
public final class GistUpdate {

    private final Gist gist;
    private final Map<String, @Nullable GitHubUpdateGistFile> files = new LinkedHashMap<>();
    private @Nullable String description;
    private final UpdateState state = new UpdateState();

    GistUpdate(final Gist gist) {
        this.gist = gist;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GistUpdate description(final String description) {
        this.description = requireNonNull(description, "description");
        return changed();
    }

    /**
     * Writes a file, creating it or replacing its content.
     *
     * @param name the file name
     * @param content the new content
     * @return this update
     * @throws NullPointerException if {@code name} or {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GistUpdate write(final String name, final String content) {
        files.put(requireNotBlank(name, "name"), new GitHubUpdateGistFile(requireNonNull(content, "content"), null));
        return changed();
    }

    /**
     * Renames a file and keeps its content.
     *
     * @param name the current file name
     * @param newName the new file name
     * @return this update
     * @throws NullPointerException if {@code name} or {@code newName} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code newName} is blank
     */
    public GistUpdate rename(final String name, final String newName) {
        files.put(requireNotBlank(name, "name"), new GitHubUpdateGistFile(null, requireNotBlank(newName, "newName")));
        return changed();
    }

    /**
     * Deletes a file.
     *
     * @param name the file name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GistUpdate deleteFile(final String name) {
        files.put(requireNotBlank(name, "name"), null);
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /gists/{gist_id}}). The gist this update was made from is not changed.
     *
     * @return the gist as GitHub returned it after the update
     * @throws IllegalStateException if nothing was chosen
     * @throws IOException if the transport fails
     */
    public Gist send() throws IOException {
        state.requireChange();
        final Map<String, GitHubUpdateGistFile> sentFiles = files.isEmpty() ? null : files;
        return new Gist(
                gist.sdk(),
                gist.github().gists().gist().updateGist(gist.id(), new GitHubUpdatePayload(description, sentFiles)));
    }

    private GistUpdate changed() {
        state.mark();
        return this;
    }
}
