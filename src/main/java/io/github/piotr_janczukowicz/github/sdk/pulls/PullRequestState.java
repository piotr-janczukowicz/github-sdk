package io.github.piotr_janczukowicz.github.sdk.pulls;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;

/**
 * The state of a {@link PullRequest}. A merged pull request is {@link #CLOSED}; use {@link PullRequest#isMerged()} to
 * tell the two apart. The client decodes only these two values, so there is no fallback constant.
 */
public enum PullRequestState {
    /** The pull request is open. */
    OPEN,
    /** The pull request is closed or merged. */
    CLOSED;

    /**
     * Maps the state the client decoded.
     *
     * @param value the client state
     * @return the state
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static PullRequestState from(final GitHubPullRequestUpdateState value) {
        return switch (requireNonNull(value, "value")) {
            case OPEN -> OPEN;
            case CLOSED -> CLOSED;
        };
    }
}
