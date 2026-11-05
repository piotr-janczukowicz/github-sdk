package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamMember;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamMembership;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamRole;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpdateTeamPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpsertTeamMembershipPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubUpsertTeamRepositoryPermissionPayload;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A team of an {@link Organization}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Team {

    private static final String LOGIN = "login";
    private static final String REPOSITORY = "repository";

    private final Organization organization;
    private final GitHubTeamFull response;

    Team(final Organization organization, final GitHubTeamFull response) {
        this.organization = requireNonNull(organization, "organization");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the organization the team belongs to.
     *
     * @return the organization
     */
    public Organization organization() {
        return organization;
    }

    /**
     * Returns the team slug.
     *
     * @return the slug
     */
    public String slug() {
        return response.slug();
    }

    /**
     * Returns the team name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Lists the members ({@code GET /orgs/{org}/teams/{team_slug}/members}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubTeamMember> members() throws IOException {
        return github().teams().members().listMembers(organization.login(), slug()).stream();
    }

    /**
     * Lists the repositories the team can access ({@code GET /orgs/{org}/teams/{team_slug}/repos}). The repositories
     * are handles built from GitHub's list and hold no data; call {@link Repository#fetch()} to load one. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the repository handles
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories() throws IOException {
        return github().teams().team().listRepositories(organization.login(), slug()).stream()
                .map(listed -> organization.sdk().repository(listed.owner().login(), listed.name()));
    }

    /**
     * Lists the child teams ({@code GET /orgs/{org}/teams/{team_slug}/teams}). GitHub's list holds only a summary of
     * each team, so every listed team is loaded with one more request as the stream reaches it; a {@code limit} on the
     * stream bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException}, and a
     * listed team that is gone before it is loaded is skipped.
     *
     * @return the child teams
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Team> childTeams() throws IOException {
        return github().teams().team().listChildTeams(organization.login(), slug()).stream()
                .map(organization::loadTeam)
                .flatMap(Optional::stream);
    }

    /**
     * Changes the team ({@code PATCH /orgs/{org}/teams/{team_slug}}). This object is not changed.
     *
     * @param payload the changes, as the client defines them
     * @return the team as GitHub holds it afterwards
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Team update(final GitHubUpdateTeamPayload payload) throws IOException {
        return new Team(
                organization,
                github().teams().team().update(organization.login(), slug(), requireNonNull(payload, "payload")));
    }

    /**
     * Deletes the team ({@code DELETE /orgs/{org}/teams/{team_slug}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().teams().team().delete(organization.login(), slug());
    }

    /**
     * Adds a user to the team as a regular member. GitHub's endpoint is an upsert, so this also sets the role of an
     * existing maintainer to member; check {@link #membership(String)} first or use {@link #addMember(String,
     * GitHubTeamRole)} to keep a role. See {@link #addMember(String, GitHubTeamRole)}.
     *
     * @param login the login of the user
     * @return the membership as GitHub holds it afterwards
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public GitHubTeamMembership addMember(final String login) throws IOException {
        return github().teams().members().upsertMembership(organization.login(), slug(), requireNotBlank(login, LOGIN));
    }

    /**
     * Adds a user to the team or changes their role ({@code PUT /orgs/{org}/teams/{team_slug}/memberships/{username}}).
     * A user who is not a member of the organization receives an invitation, and the membership is pending until they
     * accept.
     *
     * @param login the login of the user
     * @param role the role in the team
     * @return the membership as GitHub holds it afterwards
     * @throws NullPointerException if {@code login} or {@code role} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public GitHubTeamMembership addMember(final String login, final GitHubTeamRole role) throws IOException {
        return github().teams()
                .members()
                .upsertMembership(
                        organization.login(),
                        slug(),
                        requireNotBlank(login, LOGIN),
                        new GitHubUpsertTeamMembershipPayload(requireNonNull(role, "role")));
    }

    /**
     * Removes a user from the team ({@code DELETE /orgs/{org}/teams/{team_slug}/memberships/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void removeMember(final String login) throws IOException {
        github().teams().members().removeMembership(organization.login(), slug(), requireNotBlank(login, LOGIN));
    }

    /**
     * Loads the membership of a user in the team ({@code GET /orgs/{org}/teams/{team_slug}/memberships/{username}}) as
     * GitHub returns it.
     *
     * @param login the login of the user
     * @return the membership, or empty when the user has none or GitHub answers HTTP 404
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubTeamMembership> membership(final String login) throws IOException {
        return github().teams().members().getMembership(organization.login(), slug(), requireNotBlank(login, LOGIN));
    }

    /**
     * Gives the team access to a repository with the permission the organization sets by default. See
     * {@link #addRepository(Repository, RepositoryPermission)}.
     *
     * @param repository the repository
     * @throws NullPointerException if {@code repository} is {@code null}
     * @throws IOException if the transport fails
     */
    public void addRepository(final Repository repository) throws IOException {
        final Repository target = requireNonNull(repository, REPOSITORY);
        github().teams().team().upsertRepositoryPermission(organization.login(), slug(), target.owner(), target.name());
    }

    /**
     * Gives the team access to a repository, or changes its permission ({@code PUT
     * /orgs/{org}/teams/{team_slug}/repos/{owner}/{repo}}).
     *
     * @param repository the repository
     * @param permission the permission of the team on the repository
     * @throws NullPointerException if {@code repository} or {@code permission} is {@code null}
     * @throws IOException if the transport fails
     */
    public void addRepository(final Repository repository, final RepositoryPermission permission) throws IOException {
        final Repository target = requireNonNull(repository, REPOSITORY);
        github().teams()
                .team()
                .upsertRepositoryPermission(
                        organization.login(),
                        slug(),
                        target.owner(),
                        target.name(),
                        new GitHubUpsertTeamRepositoryPermissionPayload(
                                ClientValues.permissionToClient(requireNonNull(permission, "permission"))
                                        .wireValue()));
    }

    /**
     * Takes a repository away from the team ({@code DELETE /orgs/{org}/teams/{team_slug}/repos/{owner}/{repo}}).
     *
     * @param repository the repository
     * @throws NullPointerException if {@code repository} is {@code null}
     * @throws IOException if the transport fails
     */
    public void removeRepository(final Repository repository) throws IOException {
        final Repository target = requireNonNull(repository, REPOSITORY);
        github().teams().team().removeRepository(organization.login(), slug(), target.owner(), target.name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubTeamFull response() {
        return response;
    }

    private GitHub github() {
        return organization.sdk().github();
    }
}
