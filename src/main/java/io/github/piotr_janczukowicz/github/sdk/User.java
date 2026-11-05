package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationSimple;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.users.GitHubAuthenticatedUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubKeySimple;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubSocialAccount;
import io.github.piotr_janczukowicz.github.api.users.GitHubSshSigningKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.gists.Gist;
import io.github.piotr_janczukowicz.github.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackage;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackages;
import io.github.piotr_janczukowicz.github.sdk.projects.Project;
import io.github.piotr_janczukowicz.github.sdk.projects.Projects;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A GitHub user, as GitHub returned it. The object is immutable, and {@link #response()} gives the complete response
 * for every field the accessors here do not cover.
 */
public final class User {

    private final GitHubSdk sdk;
    private final GitHubUserView response;

    /**
     * Wraps a response. Applications normally get this object from {@code GitHubSdk.user(String)}; the constructor is
     * public so that the account objects of other packages can wrap the profile they update.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public User(final GitHubSdk sdk, final GitHubUserView response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the login name.
     *
     * @return the login
     */
    public String login() {
        return switch (response) {
            case final GitHubUser user -> user.login();
            case final GitHubAuthenticatedUser user -> user.login();
        };
    }

    /**
     * Returns the display name.
     *
     * @return the name, or empty when the user has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(
                switch (response) {
                    case final GitHubUser user -> user.name();
                    case final GitHubAuthenticatedUser user -> user.name();
                });
    }

    /**
     * Lists the accounts that follow the user ({@code GET /users/{username}/followers}) as GitHub returns them. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the followers
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> followers() throws IOException {
        return sdk.github().users().followers().listFollowers(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the accounts the user follows ({@code GET /users/{username}/following}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the followed accounts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> following() throws IOException {
        return sdk.github().users().followers().listFollowing(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the public SSH keys of the user ({@code GET /users/{username}/keys}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubKeySimple> sshKeys() throws IOException {
        return sdk.github().users().keys().listPublicKeys(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the public GPG keys of the user ({@code GET /users/{username}/gpg_keys}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubGpgKey> gpgKeys() throws IOException {
        return sdk.github().users().gpgKeys().listGpgKeys(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the SSH signing keys of the user ({@code GET /users/{username}/ssh_signing_keys}) as GitHub returns them.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSshSigningKey> signingKeys() throws IOException {
        return sdk.github().users().sshSigningKeys().listSshSigningKeys(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the social accounts on the profile of the user ({@code GET /users/{username}/social_accounts}) as GitHub
     * returns them. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the social accounts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSocialAccount> socialAccounts() throws IOException {
        return sdk.github().users().socialAccounts().listSocialAccounts(login(), GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the public gists of the user ({@code GET /users/{username}/gists}). GitHub's list holds no file contents,
     * so each gist is loaded with one more request as the stream reaches it; {@code limit(...)} bounds the cost. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the gists, most recently updated first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Gist> gists() throws IOException {
        return sdk.github().gists().gist().listGistsForUser(login(), GitHubPageRequest.byPage()).stream()
                .map(sdk::load);
    }

    /**
     * Lists the public repositories of the user ({@code GET /users/{username}/repos}). The repositories are handles
     * built from GitHub's list and hold no data; call {@link Repository#fetch()} to load one. Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the repository handles
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories() throws IOException {
        return sdk.github().repositories().repository().listForUser(login()).stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Lists the repositories the user starred ({@code GET /users/{username}/starred}). The elements are handles that
     * are not loaded; call {@link Repository#fetch()} to load one. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return handles of the starred repositories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> starredRepositories() throws IOException {
        return sdk.github().activity().starring().listReposStarredByUser(login()).stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Lists the repositories the user watches ({@code GET /users/{username}/subscriptions}). The elements are handles
     * that are not loaded; call {@link Repository#fetch()} to load one. Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return handles of the watched repositories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> watchedRepositories() throws IOException {
        return sdk.github().activity().watching().listReposWatchedByUser(login()).stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Lists the public organizations of the user ({@code GET /users/{username}/orgs}). GitHub's list holds only a
     * summary of each organization, so every listed organization is loaded with one more request as the stream reaches
     * it; a {@code limit} on the stream bounds the cost. A transport failure during iteration is an
     * {@link java.io.UncheckedIOException}, and a listed organization that is gone before it is loaded is an
     * {@link IllegalStateException}.
     *
     * @return the organizations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Organization> organizations() throws IOException {
        return sdk.github().organizations().organization().listForUser(login()).stream()
                .map(this::loadOrganization);
    }

    /**
     * Lists the public events the user performed, newest first ({@code GET /users/{username}/events/public}). GitHub
     * keeps only the last 90 days of events, at most 300, and caches the feed for a few seconds, so a new event can
     * take a moment to show. The stream is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> events() throws IOException {
        return sdk.github().activity().events().listPublicEventsForUser(login()).stream();
    }

    /**
     * Lists the public events the user received, from what the user watches and follows ({@code GET
     * /users/{username}/received_events/public}). GitHub keeps only the last 90 days of events, at most 300, and caches
     * the feed for a few seconds, so a new event can take a moment to show. The stream is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> receivedEvents() throws IOException {
        return sdk.github().activity().events().listReceivedPublicEventsForUser(login()).stream();
    }

    /**
     * Lists the packages of the user of one type ({@code GET /users/{username}/packages}). GitHub requires the type,
     * and its REST API for packages needs a classic token. The stream is lazy and can be used once.
     *
     * @param type the package type
     * @return the packages
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryPackage> registryPackages(final GitHubPackageType type) throws IOException {
        return RegistryPackages.forUser(sdk, login()).all(type);
    }

    /**
     * Loads one package of the user ({@code GET /users/{username}/packages/{package_type}/{package_name}}).
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
        return RegistryPackages.forUser(sdk, login()).find(type, name);
    }

    /**
     * Lists the projects (Projects v2) of the user that the caller can see ({@code GET /users/{username}/projectsV2}).
     * The stream is lazy and can be used once.
     *
     * @return the projects
     * @throws IOException if the transport fails
     */
    public Stream<Project> projects() throws IOException {
        return Projects.forUser(sdk, login()).all();
    }

    /**
     * Loads one project by its number ({@code GET /users/{username}/projectsV2/{project_number}}).
     *
     * @param number the project number, as in the project URL
     * @return the project, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Project> project(final long number) throws IOException {
        return Projects.forUser(sdk, login()).find(number);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record, a plain user or the authenticated user
     */
    public GitHubUserView response() {
        return response;
    }

    private Organization loadOrganization(final GitHubOrganizationSimple listed) {
        return Lookups.require(
                () -> sdk.organization(listed.login()), () -> "Organization " + listed.login() + " is gone");
    }
}
