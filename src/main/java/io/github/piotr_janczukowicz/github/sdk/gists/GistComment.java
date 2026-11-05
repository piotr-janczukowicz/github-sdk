package io.github.piotr_janczukowicz.github.sdk.gists;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.gists.GitHubGistComment;
import io.github.piotr_janczukowicz.github.api.gists.GitHubUpdateCommentPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.Optional;

/**
 * A comment on a {@link Gist}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class GistComment {

    private final Gist gist;
    private final GitHubGistComment response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param gist the gist the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GistComment(final Gist gist, final GitHubGistComment response) {
        this.gist = requireNonNull(gist, "gist");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the gist the comment belongs to.
     *
     * @return the gist
     */
    public Gist gist() {
        return gist;
    }

    /**
     * Returns the comment id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the comment text.
     *
     * @return the body
     */
    public String body() {
        return response.body();
    }

    /**
     * Returns the login of the author.
     *
     * @return the login, or empty when GitHub reports no author
     */
    public Optional<String> author() {
        return Optional.ofNullable(response.user()).map(GitHubSimpleUser::login);
    }

    /**
     * Loads the comment again ({@code GET /gists/{gist_id}/comments/{comment_id}}). This object is not changed.
     *
     * @return the comment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the comment is gone
     */
    public GistComment refresh() throws IOException {
        return gist.findComment(id()).orElseThrow(() -> new IllegalStateException("Gist comment " + id() + " is gone"));
    }

    /**
     * Changes the text ({@code PATCH /gists/{gist_id}/comments/{comment_id}}). This object is not changed.
     *
     * @param body the new text
     * @return the comment as GitHub holds it afterwards
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public GistComment edit(final String body) throws IOException {
        return new GistComment(
                gist,
                gist.github()
                        .gists()
                        .comments()
                        .updateComment(gist.id(), id(), new GitHubUpdateCommentPayload(requireNotBlank(body, "body"))));
    }

    /**
     * Deletes the comment ({@code DELETE /gists/{gist_id}/comments/{comment_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gist.github().gists().comments().deleteComment(gist.id(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubGistComment response() {
        return response;
    }
}
