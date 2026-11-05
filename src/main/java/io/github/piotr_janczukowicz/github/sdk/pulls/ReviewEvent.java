package io.github.piotr_janczukowicz.github.sdk.pulls;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewEvent;

/** What a {@link PullRequestReview} says when it is submitted. */
public enum ReviewEvent {
    /** Approves the pull request. */
    APPROVE,
    /** Asks for changes before the pull request can be merged. */
    REQUEST_CHANGES,
    /** Leaves feedback without approving or blocking. */
    COMMENT;

    GitHubPullRequestReviewEvent toClient() {
        return switch (this) {
            case APPROVE -> GitHubPullRequestReviewEvent.APPROVE;
            case REQUEST_CHANGES -> GitHubPullRequestReviewEvent.REQUEST_CHANGES;
            case COMMENT -> GitHubPullRequestReviewEvent.COMMENT;
        };
    }
}
