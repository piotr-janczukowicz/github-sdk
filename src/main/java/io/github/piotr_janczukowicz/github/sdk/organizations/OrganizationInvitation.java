package io.github.piotr_janczukowicz.github.sdk.organizations;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.teams.GitHubOrganizationInvitation;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A pending invitation to an organization, as GitHub returned it. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover. The client types the role as text, so it
 * is kept as GitHub wrote it, for example {@code direct_member}.
 */
public final class OrganizationInvitation {

    private final GitHubSdk sdk;
    private final String organization;
    private final GitHubOrganizationInvitation response;

    public OrganizationInvitation(
            final GitHubSdk sdk, final String organization, final GitHubOrganizationInvitation response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.organization = requireNotBlank(organization, "organization");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the login of the organization that sent the invitation.
     *
     * @return the organization login
     */
    public String organization() {
        return organization;
    }

    /**
     * Returns the invitation id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the login of the invited user.
     *
     * @return the login, or empty for an invitation sent to an email address
     */
    public Optional<String> login() {
        return Optional.ofNullable(response.login());
    }

    /**
     * Returns the email address the invitation was sent to.
     *
     * @return the address, or empty for an invitation sent to a user
     */
    public Optional<String> email() {
        return Optional.ofNullable(response.email());
    }

    /**
     * Returns the role the invitation offers, as GitHub wrote it.
     *
     * @return the role
     */
    public String role() {
        return response.role();
    }

    /**
     * Tells whether sending the invitation failed.
     *
     * @return {@code true} when GitHub reports a failure time
     */
    public boolean isFailed() {
        return response.failedAt() != null;
    }

    /**
     * Cancels the invitation ({@code DELETE /orgs/{org}/invitations/{invitation_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void cancel() throws IOException {
        final GitHub github = Internals.github(sdk);
        github.organizations().members().cancelInvitation(organization, id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubOrganizationInvitation response() {
        return response;
    }
}
