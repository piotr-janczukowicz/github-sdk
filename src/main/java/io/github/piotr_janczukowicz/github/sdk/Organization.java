package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateOrgVariablePayload;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubCreateOrgWebhookPayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubCreateOrganizationInvitationPayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgMembership;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsSecret;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubSetOrganizationMembershipPayload;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateInOrgPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.github.api.teams.GitHubCreateTeamPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationInvitation;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationVariable;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationWebhook;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackage;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackages;
import io.github.piotr_janczukowicz.github.sdk.projects.Project;
import io.github.piotr_janczukowicz.github.sdk.projects.Projects;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A GitHub organization, as GitHub returned it. The object is immutable, and {@link #response()} gives the complete
 * response for every field the accessors here do not cover.
 */
public final class Organization {

    private static final String LOGIN = "login";
    private static final String ROLE = "role";
    private static final String VARIABLE_NAME = "variableName";

    private final GitHubSdk sdk;
    private final GitHubOrganizationFull response;

    Organization(final GitHubSdk sdk, final GitHubOrganizationFull response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the login name.
     *
     * @return the login
     */
    public String login() {
        return response.login();
    }

    /**
     * Returns the display name.
     *
     * @return the name, or empty when the organization has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Lists the repositories of the organization that the credentials can see ({@code GET /orgs/{org}/repos}). The
     * repositories are handles built from GitHub's list and hold no data; call {@link Repository#fetch()} to load one.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the repository handles
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories() throws IOException {
        return sdk.github().repositories().repository().listForOrg(login()).stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Creates a repository in the organization ({@code POST /orgs/{org}/repos}).
     *
     * @param payload the repository to create
     * @return the new repository, loaded with what GitHub returned
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Repository createRepository(final GitHubCreateInOrgPayload payload) throws IOException {
        final GitHubFullRepository created =
                sdk.github().repositories().repository().createInOrg(login(), requireNonNull(payload, "payload"));
        return new Repository(sdk, login(), created.name(), created);
    }

    /**
     * Lists the members ({@code GET /orgs/{org}/members}) as GitHub returns them. Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> members() throws IOException {
        return sdk.github().organizations().members().listMembers(login()).stream();
    }

    /**
     * Loads one team ({@code GET /orgs/{org}/teams/{team_slug}}).
     *
     * @param slug the team slug
     * @return the team, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code slug} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Team> team(final String slug) throws IOException {
        return sdk.github()
                .teams()
                .team()
                .getByName(login(), requireNotBlank(slug, "slug"))
                .map(loaded -> new Team(this, loaded));
    }

    /**
     * Lists the teams ({@code GET /orgs/{org}/teams}). GitHub's list holds only a summary of each team, so every listed
     * team is loaded with one more request as the stream reaches it; a {@code limit} on the stream bounds the cost. A
     * transport failure during iteration is an {@link java.io.UncheckedIOException}, and a listed team that is gone
     * before it is loaded is an {@link IllegalStateException}.
     *
     * @return the teams
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Team> teams() throws IOException {
        return sdk.github().teams().team().list(login()).stream().map(this::loadTeam);
    }

    /**
     * Tells whether a user is a member ({@code GET /orgs/{org}/members/{username}}).
     *
     * @param login the login of the user
     * @return {@code true} when the user is a member
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public boolean isMember(final String login) throws IOException {
        return sdk.github().organizations().members().isMember(this.login(), requireNotBlank(login, LOGIN));
    }

    /**
     * Loads the membership of a user ({@code GET /orgs/{org}/memberships/{username}}) as GitHub returns it.
     *
     * @param login the login of the user
     * @return the membership, or empty when the user has none or GitHub answers HTTP 404
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubOrgMembership> membership(final String login) throws IOException {
        return sdk.github().organizations().members().getMembership(this.login(), requireNotBlank(login, LOGIN));
    }

    /**
     * Adds a user to the organization or changes their role ({@code PUT /orgs/{org}/memberships/{username}}). A user
     * who is not a member yet receives an invitation, and the membership is pending until they accept.
     *
     * @param login the login of the user
     * @param role the role of the user
     * @return the membership as GitHub holds it afterwards
     * @throws NullPointerException if {@code login} or {@code role} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public GitHubOrgMembership changeMembership(final String login, final OrganizationRole role) throws IOException {
        return sdk.github()
                .organizations()
                .members()
                .setMembership(
                        this.login(),
                        requireNotBlank(login, LOGIN),
                        new GitHubSetOrganizationMembershipPayload(
                                ClientValues.organizationRoleToClient(requireNonNull(role, "role"))));
    }

    /**
     * Removes a user from the organization ({@code DELETE /orgs/{org}/members/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void removeMember(final String login) throws IOException {
        sdk.github().organizations().members().removeMember(this.login(), requireNotBlank(login, LOGIN));
    }

    /**
     * Lists the outside collaborators ({@code GET /orgs/{org}/outside_collaborators}), people who can access some
     * repositories of the organization without being members, as GitHub returns them. Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the outside collaborators
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> outsideCollaborators() throws IOException {
        return sdk
                .github()
                .organizations()
                .outsideCollaborators()
                .listOutsideCollaborators(login(), GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Invites a person by email address ({@code POST /orgs/{org}/invitations}).
     *
     * @param email the email address
     * @param role the role the invitation offers
     * @return the invitation
     * @throws NullPointerException if {@code email} or {@code role} is {@code null}
     * @throws IllegalArgumentException if {@code email} is blank
     * @throws IOException if the transport fails
     */
    public OrganizationInvitation inviteByEmail(final String email, final InvitationRole role) throws IOException {
        return invite(new GitHubCreateOrganizationInvitationPayload(
                null,
                requireNotBlank(email, "email"),
                ClientValues.invitationRoleToClient(requireNonNull(role, ROLE)),
                null));
    }

    /**
     * Invites a user by their numeric id ({@code POST /orgs/{org}/invitations}).
     *
     * @param userId the id of the user
     * @param role the role the invitation offers
     * @return the invitation
     * @throws NullPointerException if {@code role} is {@code null}
     * @throws IOException if the transport fails
     */
    public OrganizationInvitation inviteUser(final long userId, final InvitationRole role) throws IOException {
        return invite(new GitHubCreateOrganizationInvitationPayload(
                userId, null, ClientValues.invitationRoleToClient(requireNonNull(role, ROLE)), null));
    }

    /**
     * Lists the pending invitations ({@code GET /orgs/{org}/invitations}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the invitations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<OrganizationInvitation> invitations() throws IOException {
        return sdk.github().organizations().members().listPendingInvitations(login()).stream()
                .map(listed -> new OrganizationInvitation(sdk, login(), listed));
    }

    /**
     * Creates a team with a name only ({@code POST /orgs/{org}/teams}). See
     * {@link #createTeam(GitHubCreateTeamPayload)} for the other options.
     *
     * @param name the name of the team
     * @return the new team
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Team createTeam(final String name) throws IOException {
        return createTeam(new GitHubCreateTeamPayload(
                requireNotBlank(name, "name"), null, null, null, null, null, null, null, null));
    }

    /**
     * Creates a team with every option the client offers ({@code POST /orgs/{org}/teams}).
     *
     * @param payload the name and options of the team
     * @return the new team
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Team createTeam(final GitHubCreateTeamPayload payload) throws IOException {
        return new Team(this, sdk.github().teams().team().create(login(), requireNonNull(payload, "payload")));
    }

    /**
     * Lists the webhooks of the organization ({@code GET /orgs/{org}/hooks}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the webhooks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<OrganizationWebhook> webhooks() throws IOException {
        return sdk.github().organizations().webhooks().listWebhooks(login(), GitHubPageRequest.byPage()).stream()
                .map(listed -> new OrganizationWebhook(sdk, login(), listed));
    }

    /**
     * Loads one webhook of the organization ({@code GET /orgs/{org}/hooks/{hook_id}}).
     *
     * @param id the webhook id
     * @return the webhook, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<OrganizationWebhook> webhook(final long id) throws IOException {
        return sdk.github()
                .organizations()
                .webhooks()
                .getWebhook(login(), id)
                .map(loaded -> new OrganizationWebhook(sdk, login(), loaded));
    }

    /**
     * Creates a webhook of the organization ({@code POST /orgs/{org}/hooks}).
     *
     * @param payload the name, delivery configuration, events and state of the webhook
     * @return the new webhook
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public OrganizationWebhook createWebhook(final GitHubCreateOrgWebhookPayload payload) throws IOException {
        return new OrganizationWebhook(
                sdk,
                login(),
                sdk.github().organizations().webhooks().createWebhook(login(), requireNonNull(payload, "payload")));
    }

    /**
     * Lists the Actions variables of the organization ({@code GET /orgs/{org}/actions/variables}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the variables
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<OrganizationVariable> variables() throws IOException {
        return sdk.github().actions().variables().listOrgVariables(login(), GitHubPageRequest.byPage()).stream()
                .map(listed -> new OrganizationVariable(sdk, login(), listed));
    }

    /**
     * Loads one Actions variable of the organization ({@code GET /orgs/{org}/actions/variables/{name}}).
     *
     * @param variableName the variable name
     * @return the variable, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code variableName} is {@code null}
     * @throws IllegalArgumentException if {@code variableName} is blank
     * @throws IOException if the transport fails
     */
    public Optional<OrganizationVariable> variable(final String variableName) throws IOException {
        return sdk.github()
                .actions()
                .variables()
                .getOrgVariable(login(), requireNotBlank(variableName, VARIABLE_NAME))
                .map(loaded -> new OrganizationVariable(sdk, login(), loaded));
    }

    /**
     * Creates an Actions variable of the organization ({@code POST /orgs/{org}/actions/variables}).
     *
     * @param variableName the variable name
     * @param value the value
     * @param visibility which repositories can read the variable
     * @return the variable as GitHub holds it afterwards
     * @throws NullPointerException if {@code variableName}, {@code value} or {@code visibility} is {@code null}
     * @throws IllegalArgumentException if {@code variableName} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the variable it just created
     */
    public OrganizationVariable createVariable(
            final String variableName, final String value, final VariableVisibility visibility) throws IOException {
        final String validName = requireNotBlank(variableName, VARIABLE_NAME);
        sdk.github()
                .actions()
                .variables()
                .createOrgVariable(
                        login(),
                        new GitHubCreateOrgVariablePayload(
                                validName,
                                requireNonNull(value, "value"),
                                ClientValues.variableVisibilityToClient(requireNonNull(visibility, "visibility")),
                                null));
        return variable(validName)
                .orElseThrow(() -> new IllegalStateException("Organization variable " + validName + " is gone"));
    }

    /**
     * Lists the Actions secrets of the organization ({@code GET /orgs/{org}/actions/secrets}) as GitHub returns them.
     * GitHub never returns a secret value, only the name and the dates. Creating a secret needs the value encrypted
     * with the organization public key, which this library does not do; use the client for that. Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the secrets
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubOrganizationActionsSecret> secrets() throws IOException {
        return sdk.github().actions().secrets().listOrgSecrets(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Deletes an Actions secret of the organization ({@code DELETE /orgs/{org}/actions/secrets/{secret_name}}).
     *
     * @param secretName the secret name
     * @throws NullPointerException if {@code secretName} is {@code null}
     * @throws IllegalArgumentException if {@code secretName} is blank
     * @throws IOException if the transport fails
     */
    public void deleteSecret(final String secretName) throws IOException {
        sdk.github().actions().secrets().deleteOrgSecret(login(), requireNotBlank(secretName, "secretName"));
    }

    /**
     * Lists the public events of the organization, newest first ({@code GET /orgs/{org}/events}). GitHub keeps only the
     * last 90 days of events, at most 300, and caches the feed for a few seconds, so a new event can take a moment to
     * show. The stream is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> events() throws IOException {
        return sdk.github().activity().events().listPublicOrgEvents(login()).stream();
    }

    /**
     * Lists the packages of the organization of one type ({@code GET /orgs/{org}/packages}). GitHub requires the type,
     * and its REST API for packages needs a classic token. The stream is lazy and can be used once.
     *
     * @param type the package type
     * @return the packages
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryPackage> registryPackages(final GitHubPackageType type) throws IOException {
        return RegistryPackages.forOrganization(sdk, login()).all(type);
    }

    /**
     * Loads one package of the organization ({@code GET /orgs/{org}/packages/{package_type}/{package_name}}).
     *
     * @param type the package type
     * @param name the package name
     * @return the package, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code type} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<RegistryPackage> registryPackage(final GitHubPackageType type, final String name)
            throws IOException {
        return RegistryPackages.forOrganization(sdk, login()).find(type, name);
    }

    /**
     * Lists the repository security advisories across the organization ({@code GET /orgs/{org}/security-advisories}) as
     * GitHub returns them. The stream is lazy and can be used once.
     *
     * @return the advisories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubRepositoryAdvisory> advisories() throws IOException {
        return sdk.github().securityAdvisories().repositoryAdvisories().listOrgRepositoryAdvisories(login()).stream();
    }

    /**
     * Lists the projects (Projects v2) of the organization that the caller can see ({@code GET
     * /orgs/{org}/projectsV2}). The stream is lazy and can be used once.
     *
     * @return the projects
     * @throws IOException if the transport fails
     */
    public Stream<Project> projects() throws IOException {
        return Projects.forOrganization(sdk, login()).all();
    }

    /**
     * Loads one project by its number ({@code GET /orgs/{org}/projectsV2/{project_number}}).
     *
     * @param number the project number, as in the project URL
     * @return the project, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Project> project(final long number) throws IOException {
        return Projects.forOrganization(sdk, login()).find(number);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubOrganizationFull response() {
        return response;
    }

    GitHubSdk sdk() {
        return sdk;
    }

    Team loadTeam(final GitHubTeam listed) {
        return Lookups.require(() -> team(listed.slug()), () -> "Team " + listed.slug() + " is gone");
    }

    private OrganizationInvitation invite(final GitHubCreateOrganizationInvitationPayload payload) throws IOException {
        return new OrganizationInvitation(
                sdk, login(), sdk.github().organizations().members().createInvitation(login(), payload));
    }
}
