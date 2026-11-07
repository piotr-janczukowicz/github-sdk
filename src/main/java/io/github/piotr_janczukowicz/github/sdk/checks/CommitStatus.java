package io.github.piotr_janczukowicz.github.sdk.checks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatus;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import java.util.Optional;

/**
 * One status that a service reported for a {@link Commit}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class CommitStatus {

    private static final String SUCCESS = "success";

    private final Commit commit;
    private final GitHubCommitStatus response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param commit the commit
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CommitStatus(final Commit commit, final GitHubCommitStatus response) {
        this.commit = requireNonNull(commit, "commit");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the commit the status belongs to.
     *
     * @return the commit
     */
    public Commit commit() {
        return commit;
    }

    /**
     * Returns the status id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the name of the service or check that reported the status, for example {@code ci/build}.
     *
     * @return the context
     */
    public String context() {
        return response.context();
    }

    /**
     * Returns the state exactly as GitHub reports it: {@code error}, {@code failure}, {@code pending} or
     * {@code success}. GitHub's specification types the field as text, so the SDK does not turn it into an enum.
     *
     * @return the state
     */
    public String state() {
        return response.state();
    }

    /**
     * Tells whether the check passed.
     *
     * @return {@code true} when the state is {@code success}
     */
    public boolean isSuccess() {
        return SUCCESS.equals(response.state());
    }

    /**
     * Returns the short description the service gave.
     *
     * @return the description, or empty when there is none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the link to the service's page about the status.
     *
     * @return the URL, or empty when there is none
     */
    public Optional<String> targetUrl() {
        return Optional.ofNullable(response.targetUrl());
    }

    /**
     * Returns the user who reported the status.
     *
     * @return the creator, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> creator() {
        return Optional.ofNullable(response.creator());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCommitStatus response() {
        return response;
    }
}
