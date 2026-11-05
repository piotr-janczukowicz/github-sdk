package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComment;
import io.github.piotr_janczukowicz.github.api.commits.GitHubUpdateCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A comment on a commit of a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class CommitComment {

    private final Repository repository;
    private final GitHubCommitComment response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CommitComment(final Repository repository, final GitHubCommitComment response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the comment belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
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
     * Returns the sha of the commit the comment is on.
     *
     * @return the commit sha
     */
    public String commitId() {
        return response.commitId();
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
     * Returns the file the comment is on.
     *
     * @return the path, or empty for a comment on the commit as a whole
     */
    public Optional<String> path() {
        return Optional.ofNullable(response.path());
    }

    /**
     * Returns the line of the file the comment is on.
     *
     * @return the line, or empty for a comment that is not on a line
     */
    public OptionalInt line() {
        final Integer line = response.line();
        return line == null ? OptionalInt.empty() : OptionalInt.of(line);
    }

    /**
     * Loads the comment again ({@code GET /repos/{owner}/{repo}/comments/{comment_id}}). This object is not changed.
     *
     * @return the comment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the comment is gone
     */
    public CommitComment refresh() throws IOException {
        return repository
                .commitComment(id())
                .orElseThrow(() -> new IllegalStateException("Commit comment " + id() + " is gone"));
    }

    /**
     * Changes the text ({@code PATCH /repos/{owner}/{repo}/comments/{comment_id}}). This object is not changed.
     *
     * @param body the new text
     * @return the comment as GitHub holds it afterwards
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public CommitComment edit(final String body) throws IOException {
        final GitHub github = Internals.github(repository);
        return new CommitComment(
                repository,
                github.commits()
                        .comments()
                        .updateCommitComment(
                                repository.owner(),
                                repository.name(),
                                id(),
                                new GitHubUpdateCommitCommentPayload(requireNotBlank(body, "body"))));
    }

    /**
     * Deletes the comment ({@code DELETE /repos/{owner}/{repo}/comments/{comment_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.commits().comments().deleteCommitComment(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommitComment response() {
        return response;
    }
}
