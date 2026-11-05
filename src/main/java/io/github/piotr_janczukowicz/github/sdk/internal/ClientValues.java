package io.github.piotr_janczukowicz.github.sdk.internal;

import io.github.piotr_janczukowicz.github.api.actions.GitHubOrgVariableVisibility;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaboratorPermission;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationInvitationRole;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReleaseReactionContent;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;

/**
 * Converts the SDK's enums to the client's enums for the values that more than one package needs. Enums that only their
 * own package converts keep a package-private conversion of their own.
 */
public final class ClientValues {

    private ClientValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a reaction emoji to the client value for a release, which accepts only six of the eight emojis.
     *
     * @param content the SDK value
     * @return the client value of the same name
     * @throws IllegalArgumentException if the emoji is {@code MINUS_ONE} or {@code CONFUSED}, which GitHub does not
     *     accept on a release
     */
    public static GitHubReleaseReactionContent releaseReactionToClient(final GitHubReactionContent content) {
        return switch (content) {
            case PLUS_ONE -> GitHubReleaseReactionContent.PLUS_ONE;
            case LAUGH -> GitHubReleaseReactionContent.LAUGH;
            case HEART -> GitHubReleaseReactionContent.HEART;
            case HOORAY -> GitHubReleaseReactionContent.HOORAY;
            case ROCKET -> GitHubReleaseReactionContent.ROCKET;
            case EYES -> GitHubReleaseReactionContent.EYES;
            case MINUS_ONE, CONFUSED ->
                throw new IllegalArgumentException("A release does not accept the reaction " + content);
        };
    }

    /**
     * Converts a repository permission to the client's collaborator permission.
     *
     * @param permission the SDK value
     * @return the client value of the same name
     */
    public static GitHubCollaboratorPermission permissionToClient(final RepositoryPermission permission) {
        return switch (permission) {
            case PULL -> GitHubCollaboratorPermission.PULL;
            case TRIAGE -> GitHubCollaboratorPermission.TRIAGE;
            case PUSH -> GitHubCollaboratorPermission.PUSH;
            case MAINTAIN -> GitHubCollaboratorPermission.MAINTAIN;
            case ADMIN -> GitHubCollaboratorPermission.ADMIN;
        };
    }

    /**
     * Converts an invitation role to the client's invitation role.
     *
     * @param role the SDK value
     * @return the client value of the same name
     */
    public static GitHubOrganizationInvitationRole invitationRoleToClient(final InvitationRole role) {
        return switch (role) {
            case DIRECT_MEMBER -> GitHubOrganizationInvitationRole.DIRECT_MEMBER;
            case ADMIN -> GitHubOrganizationInvitationRole.ADMIN;
            case BILLING_MANAGER -> GitHubOrganizationInvitationRole.BILLING_MANAGER;
        };
    }

    /**
     * Converts a variable visibility to the client's value.
     *
     * @param visibility the SDK value
     * @return the client value of the same name
     */
    public static GitHubOrgVariableVisibility variableVisibilityToClient(final VariableVisibility visibility) {
        return switch (visibility) {
            case ALL -> GitHubOrgVariableVisibility.ALL;
            case PRIVATE -> GitHubOrgVariableVisibility.PRIVATE;
        };
    }
}
