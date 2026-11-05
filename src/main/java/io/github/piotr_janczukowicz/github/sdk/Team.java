package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamMember;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * A team of an {@link Organization}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Team {

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
        return organization.sdk().github().teams().members().listMembers(organization.login(), slug()).stream();
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
        return organization.sdk().github().teams().team().listRepositories(organization.login(), slug()).stream()
                .map(listed -> organization.sdk().repository(listed.owner().login(), listed.name()));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubTeamFull response() {
        return response;
    }
}
