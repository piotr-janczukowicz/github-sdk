package io.github.piotr_janczukowicz.github.sdk.gists;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistCommit;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleFile;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A gist, as GitHub returned it. The object is immutable, and {@link #response()} gives the complete response for every
 * field the accessors here do not cover.
 */
public final class Gist {

    private static final String SHA = "sha";

    private final GitHubSdk sdk;
    private final GitHubGistSimple response;

    public Gist(final GitHubSdk sdk, final GitHubGistSimple response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the gist id.
     *
     * @return the id
     */
    public String id() {
        return response.id();
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when the gist has none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Tells whether the gist is listed publicly. A secret gist can still be read by anyone who has its link.
     *
     * @return {@code true} when the gist is public
     */
    public boolean isPublic() {
        return Boolean.TRUE.equals(response.isPublic());
    }

    /**
     * Returns the link to the gist on GitHub.
     *
     * @return the URL
     */
    public String htmlUrl() {
        return response.htmlUrl();
    }

    /**
     * Returns the login of the owner.
     *
     * @return the login, or empty when the gist has no owner, which is the case for an anonymous gist
     */
    public Optional<String> owner() {
        return Optional.ofNullable(response.owner()).map(GitHubSimpleUser::login);
    }

    /**
     * Returns the files of the gist, as the client decoded them, by file name.
     *
     * @return the files, empty when GitHub reports none
     */
    public Map<String, GitHubGistSimpleFile> files() {
        final Map<String, GitHubGistSimpleFile> files = response.files();
        return files == null ? Map.of() : files;
    }

    /**
     * Returns the text of a file. GitHub cuts the content of a large file off in its responses; for such a file this
     * method returns empty, and the raw URL of the file in {@link #files()} serves the whole content.
     *
     * @param fileName the name of the file
     * @return the content, or empty when the gist has no such file or its content was cut off
     */
    public Optional<String> content(final String fileName) {
        return Optional.ofNullable(files().get(fileName))
                .filter(file -> !Boolean.TRUE.equals(file.truncated()))
                .map(GitHubGistSimpleFile::content);
    }

    /**
     * Starts a change of several fields and files at once; nothing is sent until {@link GistUpdate#send()}.
     *
     * @return a new update for this gist
     */
    public GistUpdate update() {
        return new GistUpdate(this);
    }

    /**
     * Loads the gist again ({@code GET /gists/{gist_id}}). This object is not changed.
     *
     * @return the gist as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the gist is gone
     */
    public Gist refresh() throws IOException {
        return sdk.gist(id()).orElseThrow(() -> new IllegalStateException("Gist " + id() + " is gone"));
    }

    /**
     * Deletes the gist ({@code DELETE /gists/{gist_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().gists().gist().deleteGist(id());
    }

    /**
     * Stars the gist ({@code PUT /gists/{gist_id}/star}).
     *
     * @throws IOException if the transport fails
     */
    public void star() throws IOException {
        github().gists().gist().starGist(id());
    }

    /**
     * Removes the star of the gist ({@code DELETE /gists/{gist_id}/star}).
     *
     * @throws IOException if the transport fails
     */
    public void unstar() throws IOException {
        github().gists().gist().unstarGist(id());
    }

    /**
     * Tells whether the authenticated user starred the gist ({@code GET /gists/{gist_id}/star}).
     *
     * @return {@code true} when the gist is starred
     * @throws IOException if the transport fails
     */
    public boolean isStarred() throws IOException {
        return github().gists().gist().isGistStarred(id());
    }

    /**
     * Forks the gist into the account of the authenticated user ({@code POST /gists/{gist_id}/forks}). GitHub does not
     * let a user fork their own gist. The fork is loaded with one more request.
     *
     * @return the new gist
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the fork it just created
     */
    public Gist fork() throws IOException {
        final String forkId = github().gists().gist().forkGist(id()).id();
        return sdk.gist(forkId).orElseThrow(() -> new IllegalStateException("Gist " + forkId + " is gone"));
    }

    /**
     * Lists the forks of the gist ({@code GET /gists/{gist_id}/forks}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the forks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Gist> forks() throws IOException {
        return github().gists().gist().listGistForks(id(), GitHubPageRequest.byPage()).stream()
                .map(listed -> new Gist(sdk, listed));
    }

    /**
     * Lists the revisions of the gist ({@code GET /gists/{gist_id}/commits}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the revisions, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubGistCommit> commits() throws IOException {
        return github().gists().gist().listGistCommits(id(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Loads the gist as it was at a revision ({@code GET /gists/{gist_id}/{sha}}).
     *
     * @param sha the sha of the revision, from {@link #commits()}
     * @return the gist at that revision, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Gist> revision(final String sha) throws IOException {
        return github().gists()
                .gist()
                .getGistRevision(id(), requireNotBlank(sha, SHA))
                .map(found -> new Gist(sdk, found));
    }

    /**
     * Lists the comments ({@code GET /gists/{gist_id}/comments}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the comments, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GistComment> comments() throws IOException {
        return github().gists().comments().listComments(id(), GitHubPageRequest.byPage()).stream()
                .map(listed -> new GistComment(this, listed));
    }

    /**
     * Loads one comment ({@code GET /gists/{gist_id}/comments/{comment_id}}).
     *
     * @param commentId the comment id
     * @return the comment, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GistComment> findComment(final long commentId) throws IOException {
        return github().gists().comments().getComment(id(), commentId).map(found -> new GistComment(this, found));
    }

    /**
     * Adds a comment ({@code POST /gists/{gist_id}/comments}).
     *
     * @param body the comment text
     * @return the new comment
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public GistComment comment(final String body) throws IOException {
        return new GistComment(
                this,
                github().gists()
                        .comments()
                        .createComment(id(), new GitHubCreateCommentPayload(requireNotBlank(body, "body"))));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubGistSimple response() {
        return response;
    }

    GitHub github() {
        return Internals.github(sdk);
    }

    GitHubSdk sdk() {
        return sdk;
    }
}
