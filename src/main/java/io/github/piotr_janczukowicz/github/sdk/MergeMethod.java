package io.github.piotr_janczukowicz.github.sdk;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeMethod;

/** How a {@link PullRequest} is merged. */
public enum MergeMethod {
    /** Adds all commits to the base branch with a merge commit. */
    MERGE,
    /** Combines all commits into one commit on the base branch. */
    SQUASH,
    /** Adds all commits to the base branch one by one, without a merge commit. */
    REBASE;

    GitHubPullRequestMergeMethod toClient() {
        return switch (this) {
            case MERGE -> GitHubPullRequestMergeMethod.MERGE;
            case SQUASH -> GitHubPullRequestMergeMethod.SQUASH;
            case REBASE -> GitHubPullRequestMergeMethod.REBASE;
        };
    }
}
