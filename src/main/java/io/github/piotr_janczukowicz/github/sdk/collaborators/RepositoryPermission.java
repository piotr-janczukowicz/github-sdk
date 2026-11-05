package io.github.piotr_janczukowicz.github.sdk.collaborators;

import io.github.piotr_janczukowicz.github.api.collaborators.GitHubUpdateRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.github.sdk.Repository;

/**
 * The access level a collaborator has on a {@link Repository}. Custom roles of an organization are not covered, because
 * they are not a fixed set of values.
 */
public enum RepositoryPermission {
    /** Read access; GitHub also calls it {@code read}. */
    PULL,
    /** Read access, plus managing issues and pull requests without write access. */
    TRIAGE,
    /** Write access; GitHub also calls it {@code write}. */
    PUSH,
    /** Write access, plus managing the repository without sensitive or destructive actions. */
    MAINTAIN,
    /** Full access. */
    ADMIN;

    GitHubUpdateRepositoryInvitationPermission toInvitationUpdate() {
        return switch (this) {
            case PULL -> GitHubUpdateRepositoryInvitationPermission.READ;
            case TRIAGE -> GitHubUpdateRepositoryInvitationPermission.TRIAGE;
            case PUSH -> GitHubUpdateRepositoryInvitationPermission.WRITE;
            case MAINTAIN -> GitHubUpdateRepositoryInvitationPermission.MAINTAIN;
            case ADMIN -> GitHubUpdateRepositoryInvitationPermission.ADMIN;
        };
    }
}
