package io.github.piotr_janczukowicz.github.sdk.checks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubSimpleCommitStatus;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import java.util.List;

/**
 * The combined state of all statuses of a {@link Commit}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class CombinedStatus {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private static final String PENDING = "pending";

    private final Commit commit;
    private final GitHubCombinedCommitStatus response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param commit the commit
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CombinedStatus(final Commit commit, final GitHubCombinedCommitStatus response) {
        this.commit = requireNonNull(commit, "commit");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the commit the statuses belong to.
     *
     * @return the commit
     */
    public Commit commit() {
        return commit;
    }

    /**
     * Returns the combined state exactly as GitHub reports it: {@code failure}, {@code pending} or {@code success}.
     * GitHub's specification types the field as text, so the SDK does not turn it into an enum.
     *
     * @return the state
     */
    public String state() {
        return response.state();
    }

    /**
     * Tells whether every status passed.
     *
     * @return {@code true} when the combined state is {@code success}
     */
    public boolean isSuccess() {
        return SUCCESS.equals(response.state());
    }

    /**
     * Tells whether at least one status failed or errored.
     *
     * @return {@code true} when the combined state is {@code failure}
     */
    public boolean isFailure() {
        return FAILURE.equals(response.state());
    }

    /**
     * Tells whether the statuses have not all finished.
     *
     * @return {@code true} when the combined state is {@code pending}
     */
    public boolean isPending() {
        return PENDING.equals(response.state());
    }

    /**
     * Returns the latest status of each context. GitHub returns a summary of each status here, so the client's records
     * are passed through; use {@link Commit#statuses()} for the full objects.
     *
     * @return the statuses, one per context
     */
    public List<GitHubSimpleCommitStatus> statuses() {
        return response.statuses();
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCombinedCommitStatus response() {
        return response;
    }
}
