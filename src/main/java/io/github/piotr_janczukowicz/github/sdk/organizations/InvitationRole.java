package io.github.piotr_janczukowicz.github.sdk.organizations;

/** The role an invitation to an organization offers. */
public enum InvitationRole {
    /** A regular member. */
    DIRECT_MEMBER,
    /** An owner of the organization. */
    ADMIN,
    /** A billing manager, who can manage billing but not the code. */
    BILLING_MANAGER
}
