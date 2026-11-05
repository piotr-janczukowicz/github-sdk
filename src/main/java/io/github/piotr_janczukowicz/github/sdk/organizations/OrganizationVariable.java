package io.github.piotr_janczukowicz.github.sdk.organizations;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubUpdateOrgVariablePayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariable;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariableVisibility;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * An Actions variable of an organization, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response.
 */
public final class OrganizationVariable {

    private final GitHubSdk sdk;
    private final String organization;
    private final GitHubOrganizationActionsVariable response;

    public OrganizationVariable(
            final GitHubSdk sdk, final String organization, final GitHubOrganizationActionsVariable response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.organization = requireNotBlank(organization, "organization");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the login of the organization the variable belongs to.
     *
     * @return the organization login
     */
    public String organization() {
        return organization;
    }

    /**
     * Returns the name of the variable.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the value of the variable.
     *
     * @return the value
     */
    public String value() {
        return response.value();
    }

    /**
     * Returns which repositories can read the variable, as the client decoded it.
     *
     * @return the visibility
     */
    public GitHubOrganizationActionsVariableVisibility visibility() {
        return response.visibility();
    }

    /**
     * Loads the variable again ({@code GET /orgs/{org}/actions/variables/{name}}). This object is not changed.
     *
     * @return the variable as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the variable is gone
     */
    public OrganizationVariable refresh() throws IOException {
        return github().actions()
                .variables()
                .getOrgVariable(organization, name())
                .map(reloaded -> new OrganizationVariable(sdk, organization, reloaded))
                .orElseThrow(() -> new IllegalStateException("Organization variable " + name() + " is gone"));
    }

    /**
     * Changes the value ({@code PATCH /orgs/{org}/actions/variables/{name}}). This object is not changed.
     *
     * @param value the new value
     * @return the variable as GitHub holds it afterwards
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the variable is gone
     */
    public OrganizationVariable update(final String value) throws IOException {
        github().actions()
                .variables()
                .updateOrgVariable(
                        organization,
                        name(),
                        new GitHubUpdateOrgVariablePayload(null, requireNonNull(value, "value"), null, null));
        return refresh();
    }

    /**
     * Deletes the variable ({@code DELETE /orgs/{org}/actions/variables/{name}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().actions().variables().deleteOrgVariable(organization, name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubOrganizationActionsVariable response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
