package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGist;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateForAuthenticatedUserPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateInOrgPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.gists.Gist;
import io.github.piotr_janczukowicz.github.sdk.gists.GistDraft;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.internal.Lookups;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Entry point of the object layer. It wraps an existing {@link GitHub} client, so authentication, flavor, GHES version
 * and limits are configured on the client as usual; the SDK has no configuration of its own.
 */
public final class GitHubSdk {

    static {
        Internals.registerSdkAccess(GitHubSdk::github);
    }

    private final GitHub github;

    private GitHubSdk(final GitHub github) {
        this.github = requireNonNull(github, "github");
    }

    /**
     * Wraps a client.
     *
     * @param github the client every object of this SDK calls
     * @return the entry point
     * @throws NullPointerException if {@code github} is {@code null}
     */
    public static GitHubSdk of(final GitHub github) {
        return new GitHubSdk(github);
    }

    /**
     * Returns a handle for a repository. It makes no request and holds no data; call {@link Repository#fetch()} to load
     * it.
     *
     * @param owner the account that owns the repository
     * @param name the repository name
     * @return the repository handle
     * @throws NullPointerException if {@code owner} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code owner} or {@code name} is blank
     */
    public Repository repository(final String owner, final String name) {
        return new Repository(this, requireNotBlank(owner, "owner"), requireNotBlank(name, "name"), null);
    }

    /**
     * Loads the authenticated user ({@code GET /user}).
     *
     * @return the user the client's credentials belong to
     * @throws IOException if the transport fails
     */
    public User currentUser() throws IOException {
        return new User(this, github.users().user().getAuthenticatedUser());
    }

    /**
     * Creates a repository in the account of the authenticated user ({@code POST /user/repos}). To create one in an
     * organization, use {@link Organization#createRepository(GitHubCreateInOrgPayload)}.
     *
     * @param payload the name and settings of the new repository
     * @return the new repository
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Repository createRepository(final GitHubCreateForAuthenticatedUserPayload payload) throws IOException {
        requireNonNull(payload, "payload");
        final GitHubFullRepository created = github.repositories().repository().createForAuthenticatedUser(payload);
        return new Repository(this, created.owner().login(), created.name(), created);
    }

    /**
     * Loads a user by login ({@code GET /users/{username}}).
     *
     * @param login the login name
     * @return the user, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<User> user(final String login) throws IOException {
        return github.users().user().getByLogin(requireNotBlank(login, "login")).map(view -> new User(this, view));
    }

    /**
     * Loads an organization by login ({@code GET /orgs/{org}}).
     *
     * @param login the organization login name
     * @return the organization, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Organization> organization(final String login) throws IOException {
        return github.organizations()
                .organization()
                .get(requireNotBlank(login, "login"))
                .map(loaded -> new Organization(this, loaded));
    }

    /**
     * Loads a gist ({@code GET /gists/{gist_id}}).
     *
     * @param id the gist id
     * @return the gist, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Gist> gist(final String id) throws IOException {
        return github.gists().gist().getGist(requireNotBlank(id, "id")).map(loaded -> new Gist(this, loaded));
    }

    /**
     * Lists the gists of the authenticated user ({@code GET /gists}). GitHub's list holds no file contents, so each
     * gist is loaded with one more request as the stream reaches it; {@code limit(...)} bounds the cost. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the gists, most recently updated first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Gist> gists() throws IOException {
        return github.gists().gist().listGists(GitHubPageRequest.byPage()).stream()
                .map(this::load);
    }

    /**
     * Lists the public gists of everyone ({@code GET /gists/public}). See {@link #gists()} for the cost per gist.
     *
     * @return the gists, most recently updated first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Gist> publicGists() throws IOException {
        return github.gists().gist().listPublicGists(GitHubPageRequest.byPage()).stream()
                .map(this::load);
    }

    /**
     * Lists the gists the authenticated user starred ({@code GET /gists/starred}). See {@link #gists()} for the cost
     * per gist.
     *
     * @return the starred gists
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Gist> starredGists() throws IOException {
        return github.gists().gist().listStarredGists(GitHubPageRequest.byPage()).stream()
                .map(this::load);
    }

    /**
     * Starts a new gist; nothing is sent until {@link GistDraft#send()}.
     *
     * @return a new, empty draft
     */
    public GistDraft createGist() {
        return new GistDraft(this);
    }

    /**
     * Returns the search of this SDK.
     *
     * @return the search
     */
    public Search search() {
        return new Search(this);
    }

    /**
     * Reads the rate limit ({@code GET /rate_limit}).
     *
     * @return the rate limit, or empty when the server does not report one, as a GitHub Enterprise Server can
     * @throws IOException if the transport fails
     */
    public Optional<RateLimit> rateLimit() throws IOException {
        return github.rateLimit().get().map(RateLimit::new);
    }

    /**
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitHub github() {
        return github;
    }

    Gist load(final GitHubBaseGist listed) {
        return Lookups.require(() -> gist(listed.id()), () -> "Gist " + listed.id() + " is gone");
    }
}
