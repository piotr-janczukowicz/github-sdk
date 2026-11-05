package io.github.piotr_janczukowicz.github.sdk.internal;

import io.github.piotr_janczukowicz.github.api.actions.GitHubOrgVariableVisibility;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaboratorPermission;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationInvitationRole;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationMembershipRole;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReleaseReactionContent;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamRole;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatusState;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.TeamRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;

/**
 * Converts the SDK's enums to the client's enums for the values that more than one package needs. Enums that only their
 * own package converts keep a package-private conversion of their own.
 */
public final class ClientValues {

    private ClientValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a reaction emoji to the client's value.
     *
     * @param content the SDK value
     * @return the client value of the same name
     */
    public static GitHubReactionContent reactionToClient(final ReactionContent content) {
        return switch (content) {
            case PLUS_ONE -> GitHubReactionContent.PLUS_ONE;
            case MINUS_ONE -> GitHubReactionContent.MINUS_ONE;
            case LAUGH -> GitHubReactionContent.LAUGH;
            case CONFUSED -> GitHubReactionContent.CONFUSED;
            case HEART -> GitHubReactionContent.HEART;
            case HOORAY -> GitHubReactionContent.HOORAY;
            case ROCKET -> GitHubReactionContent.ROCKET;
            case EYES -> GitHubReactionContent.EYES;
        };
    }

    /**
     * Converts an SDK reaction emoji to the client value for a release, which accepts only six of the eight emojis.
     *
     * @param content the SDK value
     * @return the client value of the same name
     * @throws IllegalArgumentException if the emoji is {@code MINUS_ONE} or {@code CONFUSED}, which GitHub does not
     *     accept on a release
     */
    public static GitHubReleaseReactionContent releaseReactionToClient(final ReactionContent content) {
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
     * Converts the client's reaction emoji to the SDK's value.
     *
     * @param content the client value
     * @return the SDK value of the same name
     */
    public static ReactionContent reactionFromClient(final GitHubReactionContent content) {
        return switch (content) {
            case PLUS_ONE -> ReactionContent.PLUS_ONE;
            case MINUS_ONE -> ReactionContent.MINUS_ONE;
            case LAUGH -> ReactionContent.LAUGH;
            case CONFUSED -> ReactionContent.CONFUSED;
            case HEART -> ReactionContent.HEART;
            case HOORAY -> ReactionContent.HOORAY;
            case ROCKET -> ReactionContent.ROCKET;
            case EYES -> ReactionContent.EYES;
        };
    }

    /**
     * Converts a commit status state to the client's value.
     *
     * @param state the SDK value
     * @return the client value of the same name
     */
    public static GitHubCommitStatusState statusStateToClient(final CommitStatusState state) {
        return switch (state) {
            case ERROR -> GitHubCommitStatusState.ERROR;
            case FAILURE -> GitHubCommitStatusState.FAILURE;
            case PENDING -> GitHubCommitStatusState.PENDING;
            case SUCCESS -> GitHubCommitStatusState.SUCCESS;
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
     * Converts an organization role to the client's membership role.
     *
     * @param role the SDK value
     * @return the client value of the same name
     */
    public static GitHubOrganizationMembershipRole organizationRoleToClient(final OrganizationRole role) {
        return switch (role) {
            case MEMBER -> GitHubOrganizationMembershipRole.MEMBER;
            case ADMIN -> GitHubOrganizationMembershipRole.ADMIN;
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
     * Converts a team role to the client's team role.
     *
     * @param role the SDK value
     * @return the client value of the same name
     */
    public static GitHubTeamRole teamRoleToClient(final TeamRole role) {
        return switch (role) {
            case MEMBER -> GitHubTeamRole.MEMBER;
            case MAINTAINER -> GitHubTeamRole.MAINTAINER;
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
