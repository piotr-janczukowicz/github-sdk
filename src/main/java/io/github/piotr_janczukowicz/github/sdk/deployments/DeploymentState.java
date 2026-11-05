package io.github.piotr_janczukowicz.github.sdk.deployments;

import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusState;

/** The state a {@link Deployment} is reported to be in. */
public enum DeploymentState {
    /** The deployment failed with an error. */
    ERROR,
    /** The deployment failed. */
    FAILURE,
    /** The deployment is no longer active, which GitHub requires before it can be deleted. */
    INACTIVE,
    /** The deployment is running. */
    IN_PROGRESS,
    /** The deployment is waiting for a free runner. */
    QUEUED,
    /** The deployment is waiting to start. */
    PENDING,
    /** The deployment succeeded. */
    SUCCESS;

    GitHubDeploymentStatusState toClient() {
        return switch (this) {
            case ERROR -> GitHubDeploymentStatusState.ERROR;
            case FAILURE -> GitHubDeploymentStatusState.FAILURE;
            case INACTIVE -> GitHubDeploymentStatusState.INACTIVE;
            case IN_PROGRESS -> GitHubDeploymentStatusState.IN_PROGRESS;
            case QUEUED -> GitHubDeploymentStatusState.QUEUED;
            case PENDING -> GitHubDeploymentStatusState.PENDING;
            case SUCCESS -> GitHubDeploymentStatusState.SUCCESS;
        };
    }
}
