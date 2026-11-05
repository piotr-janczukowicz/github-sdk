package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateInOrgPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A GitHub organization, as GitHub returned it. The object is immutable, and {@link #response()} gives the complete
 * response for every field the accessors here do not cover.
 */
public final class Organization {

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

    private Team loadTeam(final GitHubTeam listed) {
        try {
            return team(listed.slug())
                    .orElseThrow(() -> new IllegalStateException("Team " + listed.slug() + " is gone"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
