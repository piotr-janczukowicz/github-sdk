package io.github.piotr_janczukowicz.github.sdk.git;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRef;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubUpdateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * A git reference of a {@link Repository}, such as a branch or a tag, as GitHub returned it. The object is immutable,
 * and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Ref {

    private static final String PREFIX = "refs/";

    private final Repository repository;
    private final GitHubGitRef response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Ref(final Repository repository, final GitHubGitRef response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the reference belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the full name of the reference, for example {@code refs/heads/main}.
     *
     * @return the name
     */
    public String name() {
        return response.ref();
    }

    /**
     * Returns the sha of the object the reference points to.
     *
     * @return the sha
     */
    public String sha() {
        return response.object().sha();
    }

    /**
     * Returns the type of the object the reference points to, for example {@code commit} or {@code tag}.
     *
     * @return the type
     */
    public String type() {
        return response.object().type();
    }

    /**
     * Loads the reference again ({@code GET /repos/{owner}/{repo}/git/ref/{ref}}). This object is not changed.
     *
     * @return the reference as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the reference is gone
     */
    public Ref refresh() throws IOException {
        return repository.ref(key()).orElseThrow(() -> new IllegalStateException("Ref " + name() + " is gone"));
    }

    /**
     * Moves the reference to another object, which must be a descendant of the current one. See {@link #update(String,
     * boolean)}.
     *
     * @param sha the sha of the new target
     * @return the reference as GitHub holds it afterwards
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Ref update(final String sha) throws IOException {
        return update(sha, false);
    }

    /**
     * Moves the reference to another object ({@code PATCH /repos/{owner}/{repo}/git/refs/{ref}}). This object is not
     * changed.
     *
     * @param sha the sha of the new target
     * @param isForced {@code true} to move the reference even when the new target is not a descendant of the current
     *     one, which discards the commits in between
     * @return the reference as GitHub holds it afterwards
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Ref update(final String sha, final boolean isForced) throws IOException {
        final GitHub github = Internals.github(repository);
        return new Ref(
                repository,
                github.gitDatabase()
                        .refs()
                        .updateRef(
                                repository.owner(),
                                repository.name(),
                                key(),
                                new GitHubUpdateRefPayload(requireNotBlank(sha, "sha"), isForced)));
    }

    /**
     * Deletes the reference ({@code DELETE /repos/{owner}/{repo}/git/refs/{ref}}). Deleting a branch reference deletes
     * the branch.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.gitDatabase().refs().deleteRef(repository.owner(), repository.name(), key());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubGitRef response() {
        return response;
    }

    private String key() {
        return name().startsWith(PREFIX) ? name().substring(PREFIX.length()) : name();
    }
}
