package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDeleteFilePayload;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.jspecify.annotations.Nullable;

/**
 * A file of a {@link Repository}, as GitHub returned it at one ref. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover.
 */
public final class RepositoryFile {

    private static final String BASE64 = "base64";

    private final Repository repository;
    private final GitHubContentFile response;

    RepositoryFile(final Repository repository, final GitHubContentFile response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the file belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the path of the file within the repository.
     *
     * @return the path
     */
    public String path() {
        return response.path();
    }

    /**
     * Returns the blob hash of the file content.
     *
     * @return the SHA
     */
    public String sha() {
        return response.sha();
    }

    /**
     * Returns the size of the file as GitHub reports it.
     *
     * @return the size in bytes
     */
    public int size() {
        return response.size();
    }

    /**
     * Returns the exact content of the file, decoded from the base64 GitHub sends. Every call returns a new array.
     *
     * @return the content
     * @throws IllegalStateException if GitHub sent no content, as it does for files too large for the contents API
     */
    public byte[] bytes() {
        if (!BASE64.equals(response.encoding())) {
            throw new IllegalStateException("GitHub sent no content for " + response.path() + " (encoding "
                    + response.encoding() + "); the file is too large for the contents API");
        }
        return Base64.getMimeDecoder().decode(response.content());
    }

    /**
     * Returns the content of the file as text, decoded strictly as UTF-8.
     *
     * @return the text
     * @throws IllegalStateException if GitHub sent no content, or the content is not valid UTF-8
     */
    public String text() {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes()))
                    .toString();
        } catch (final CharacterCodingException e) {
            throw new IllegalStateException(response.path() + " is not valid UTF-8", e);
        }
    }

    /**
     * Deletes the file on the default branch ({@code DELETE /repos/{owner}/{repo}/contents/{path}}), with the blob hash
     * this object holds, so GitHub refuses the delete if the file changed since it was loaded.
     *
     * @param message the commit message
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IOException if the transport fails
     */
    public void delete(final String message) throws IOException {
        remove(requireNotBlank(message, "message"), null);
    }

    /**
     * Deletes the file on a branch ({@code DELETE /repos/{owner}/{repo}/contents/{path}}), with the blob hash this
     * object holds, so GitHub refuses the delete if the file changed since it was loaded.
     *
     * @param message the commit message
     * @param branch the branch to commit to
     * @throws IllegalArgumentException if {@code message} or {@code branch} is blank
     * @throws IOException if the transport fails
     */
    public void delete(final String message, final String branch) throws IOException {
        remove(requireNotBlank(message, "message"), requireNotBlank(branch, "branch"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubContentFile response() {
        return response;
    }

    private void remove(final String message, final @Nullable String branch) throws IOException {
        repository
                .sdk()
                .github()
                .repositories()
                .contents()
                .deleteFile(
                        repository.owner(),
                        repository.name(),
                        response.path(),
                        new GitHubDeleteFilePayload(message, response.sha(), branch, null, null));
    }
}
