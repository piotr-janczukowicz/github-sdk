package io.github.piotr_janczukowicz.github.sdk.projects;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The projects (Projects v2) of one organization or user. {@code Organization.projects()} and {@code User.projects()}
 * are the usual way in; this type exists so both owner kinds share the same project objects. GitHub's REST API can read
 * projects and manage their items, fields and views, but cannot create, change or delete a project; that needs GraphQL,
 * which this SDK does not cover.
 */
public final class Projects {

    private final ProjectGateway gateway;

    private Projects(final ProjectGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the projects of an organization; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @param login the organization login
     * @return the projects handle
     * @throws NullPointerException if {@code sdk} or {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     */
    public static Projects forOrganization(final GitHubSdk sdk, final String login) {
        return new Projects(new OrganizationProjectGateway(
                Internals.github(requireNonNull(sdk, "sdk")), requireNotBlank(login, "login")));
    }

    /**
     * Returns the projects of a user; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @param login the user login
     * @return the projects handle
     * @throws NullPointerException if {@code sdk} or {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     */
    public static Projects forUser(final GitHubSdk sdk, final String login) {
        return new Projects(
                new UserProjectGateway(Internals.github(requireNonNull(sdk, "sdk")), requireNotBlank(login, "login")));
    }

    /**
     * Lists the projects the caller can see ({@code GET /orgs/{org}/projectsV2} or {@code GET
     * /users/{username}/projectsV2}). The stream is lazy and can be used once.
     *
     * @return the projects
     * @throws IOException if the transport fails
     */
    public Stream<Project> all() throws IOException {
        return gateway.projects().map(listed -> new Project(gateway, listed));
    }

    /**
     * Loads one project by its number ({@code GET /orgs/{org}/projectsV2/{project_number}} or the user variant).
     *
     * @param number the project number, as in the project URL
     * @return the project, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Project> find(final long number) throws IOException {
        return gateway.project(number).map(loaded -> new Project(gateway, loaded));
    }
}
