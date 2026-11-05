package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.activity.GitHubListNotificationsForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGist;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicense;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicenseSimple;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateForAuthenticatedUserPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateInOrgPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubGlobalAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubListGlobalAdvisoriesFilter;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.accounts.Me;
import io.github.piotr_janczukowicz.github.sdk.apps.App;
import io.github.piotr_janczukowicz.github.sdk.apps.Marketplace;
import io.github.piotr_janczukowicz.github.sdk.gists.Gist;
import io.github.piotr_janczukowicz.github.sdk.gists.GistDraft;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import io.github.piotr_janczukowicz.github.sdk.utilities.Utilities;
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
     * Returns the account the client authenticates as, for the operations that only make sense for one's own account,
     * such as managing emails and keys. Nothing is requested until a method of the result is called.
     *
     * @return the authenticated account
     */
    public Me me() {
        return new Me(this);
    }

    /**
     * Loads a user by login ({@code GET /users/{username}}).
     *
     * @param login the login name
     * @return the user, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws NullPointerException if {@code login} is {@code null}
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
     * @throws NullPointerException if {@code login} is {@code null}
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
     * Lists the notifications of the authenticated user ({@code GET /notifications}), unread ones only. See
     * {@link #notifications(GitHubListNotificationsForAuthenticatedUserFilter)}.
     *
     * @return the notifications
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Notification> notifications() throws IOException {
        return notifications(GitHubListNotificationsForAuthenticatedUserFilter.empty());
    }

    /**
     * Lists the notifications of the authenticated user ({@code GET /notifications}). GitHub does not support
     * fine-grained tokens for notifications, so the client needs a classic token. Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for read state, participation and dates
     * @return the notifications
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Notification> notifications(final GitHubListNotificationsForAuthenticatedUserFilter filter)
            throws IOException {
        requireNonNull(filter, "filter");
        return github
                .activity()
                .notifications()
                .listNotificationsForAuthenticatedUser(filter, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Notification(this, listed));
    }

    /**
     * Loads one notification ({@code GET /notifications/threads/{thread_id}}).
     *
     * @param id the thread id
     * @return the notification, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank or not a number
     * @throws IOException if the transport fails
     */
    public Optional<Notification> notification(final String id) throws IOException {
        final long threadId = threadId(id);
        return github.activity().notifications().getThread(threadId).map(loaded -> new Notification(this, loaded));
    }

    /**
     * Marks all notifications of the authenticated user as read ({@code PUT /notifications}). GitHub may do this
     * asynchronously.
     *
     * @throws IOException if the transport fails
     */
    public void markNotificationsRead() throws IOException {
        github.activity().notifications().markNotificationsAsRead();
    }

    /**
     * Lists the commonly used open source licenses ({@code GET /licenses}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the licenses
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubLicenseSimple> licenses() throws IOException {
        return github.licenses().listCommonlyUsed(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Loads one license with its full text ({@code GET /licenses/{license}}) as GitHub returns it.
     *
     * @param key the license key, for example {@code mit}
     * @return the license, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubLicense> license(final String key) throws IOException {
        return github.licenses().get(requireNotBlank(key, "key"));
    }

    /**
     * Lists the public events on GitHub, newest first ({@code GET /events}). GitHub keeps only the last 90 days of
     * events, at most 300, and caches the feed for a few seconds, so a new event can take a moment to show. The stream
     * is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> publicEvents() throws IOException {
        return github.activity().events().listPublicEvents().stream();
    }

    /**
     * Lists the advisories of the GitHub Advisory Database, newest first ({@code GET /advisories}) as GitHub returns
     * them. The stream is lazy and can be used once.
     *
     * @return the advisories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubGlobalAdvisory> globalAdvisories() throws IOException {
        return github.securityAdvisories().globalAdvisories().listGlobalAdvisories().stream();
    }

    /**
     * Lists the advisories of the GitHub Advisory Database that match a filter, such as an ecosystem or a severity.
     *
     * @param filter the client filter record
     * @return the matching advisories
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubGlobalAdvisory> globalAdvisories(final GitHubListGlobalAdvisoriesFilter filter)
            throws IOException {
        return github
                .securityAdvisories()
                .globalAdvisories()
                .listGlobalAdvisories(requireNonNull(filter, "filter"))
                .stream();
    }

    /**
     * Loads one advisory of the GitHub Advisory Database ({@code GET /advisories/{ghsa_id}}).
     *
     * @param ghsaId the GitHub Security Advisory id, for example {@code GHSA-xxxx-xxxx-xxxx}
     * @return the advisory, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code ghsaId} is blank
     * @throws NullPointerException if {@code ghsaId} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<GitHubGlobalAdvisory> globalAdvisory(final String ghsaId) throws IOException {
        return github.securityAdvisories().globalAdvisories().getGlobalAdvisory(requireNotBlank(ghsaId, "ghsaId"));
    }

    /**
     * Loads the GitHub App the SDK is authenticated as ({@code GET /app}). It needs an App JWT, which the client builds
     * from the private key of the app; with any other token GitHub answers HTTP 401.
     *
     * @return the app
     * @throws IOException if the transport fails
     */
    public App app() throws IOException {
        return new App(this, github.apps().app().getAuthenticated());
    }

    /**
     * Loads a public GitHub App by its slug ({@code GET /apps/{app_slug}}).
     *
     * @param slug the app slug, as in the URL of the app
     * @return the app, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code slug} is blank
     * @throws NullPointerException if {@code slug} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<App> app(final String slug) throws IOException {
        return github.apps().app().getBySlug(requireNotBlank(slug, "slug")).map(loaded -> new App(this, loaded));
    }

    /**
     * Returns the Marketplace listing of the app the SDK is authenticated as. Nothing is requested until a method of
     * the result is called.
     *
     * @return the Marketplace listing
     */
    public Marketplace marketplace() {
        return new Marketplace(this);
    }

    /**
     * Returns the small endpoints that need no repository: the zen quote, the API overview, emojis, Markdown rendering
     * and the {@code .gitignore} templates. Nothing is requested until a method of the result is called.
     *
     * @return the utilities
     */
    public Utilities utilities() {
        return new Utilities(this);
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

    private static long threadId(final String id) {
        try {
            return Long.parseLong(requireNotBlank(id, "id"));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("id must be a number, but was: " + id, e);
        }
    }
}
