package io.github.piotr_janczukowicz.github.sdk;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;

/** The state a {@link Commit#createStatus(CommitStatusState, String) commit status} is set to. */
public enum CommitStatusState {
    /** The check could not run, for example because of an error in the tooling. */
    ERROR,
    /** The check ran and failed. */
    FAILURE,
    /** The check has not finished. */
    PENDING,
    /** The check ran and passed. */
    SUCCESS;

    GitHubCommitStatusState toClient() {
        return switch (this) {
            case ERROR -> GitHubCommitStatusState.ERROR;
            case FAILURE -> GitHubCommitStatusState.FAILURE;
            case PENDING -> GitHubCommitStatusState.PENDING;
            case SUCCESS -> GitHubCommitStatusState.SUCCESS;
        };
    }
}
