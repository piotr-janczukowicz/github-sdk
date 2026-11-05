package io.github.piotr_janczukowicz.github.sdk.accounts;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.apps.GitHubUserMarketplacePurchase;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubListForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreateGpgKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreatePublicSshKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreateSshSigningKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubEmail;
import io.github.piotr_janczukowicz.github.api.users.GitHubEmailVisibility;
import io.github.piotr_janczukowicz.github.api.users.GitHubSetPrimaryEmailVisibilityPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubSocialAccount;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateAuthenticatedUserPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateEmailsPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateSocialAccountsPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.User;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackage;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackages;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * The account the client authenticates as. Operations that only make sense for one's own account, such as managing
 * emails and keys, live here and nowhere else. The object holds no state and makes no request until a method is called;
 * every method asks GitHub or changes the account at once.
 */
public final class Me {

    private static final String KEY = "key";
    private static final String TITLE = "title";
    private static final String LOGIN = "login";

    private final GitHubSdk sdk;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param sdk the SDK that makes the calls
     * @throws NullPointerException if an argument is {@code null}
     */
    public Me(final GitHubSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Loads the profile of the account ({@code GET /user}).
     *
     * @return the profile
     * @throws IOException if the transport fails
     */
    public User profile() throws IOException {
        return sdk.currentUser();
    }

    /**
     * Starts a change of several profile fields at once; nothing is sent until {@link MeUpdate#send()}.
     *
     * @return a new update
     */
    public MeUpdate update() {
        return new MeUpdate(this);
    }

    /**
     * Lists the repositories the account can access ({@code GET /user/repos}), including private ones and those of
     * organizations. The repositories are handles built from GitHub's list and hold no data; call
     * {@link Repository#fetch()} to load one. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the repository handles
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories() throws IOException {
        return repositories(GitHubListForAuthenticatedUserFilter.empty());
    }

    /**
     * Lists the repositories the account can access with a filter, such as visibility or affiliation ({@code GET
     * /user/repos}). See {@link #repositories()}.
     *
     * @param filter the client filter
     * @return the repository handles
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> repositories(final GitHubListForAuthenticatedUserFilter filter) throws IOException {
        return github()
                .repositories()
                .repository()
                .listForAuthenticatedUser(requireNonNull(filter, "filter"), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Lists the email addresses of the account ({@code GET /user/emails}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the email addresses
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEmail> emails() throws IOException {
        return github().users().emails().listEmails(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Adds an email address ({@code POST /user/emails}). GitHub sends a verification mail to it.
     *
     * @param address the address
     * @return all addresses of the account afterwards
     * @throws NullPointerException if {@code address} is {@code null}
     * @throws IllegalArgumentException if {@code address} is blank
     * @throws IOException if the transport fails
     */
    public List<GitHubEmail> addEmail(final String address) throws IOException {
        return github().users()
                .emails()
                .addEmails(new GitHubUpdateEmailsPayload(List.of(requireNotBlank(address, "address"))));
    }

    /**
     * Removes an email address ({@code DELETE /user/emails}).
     *
     * @param address the address
     * @throws NullPointerException if {@code address} is {@code null}
     * @throws IllegalArgumentException if {@code address} is blank
     * @throws IOException if the transport fails
     */
    public void removeEmail(final String address) throws IOException {
        github().users()
                .emails()
                .deleteEmails(new GitHubUpdateEmailsPayload(List.of(requireNotBlank(address, "address"))));
    }

    /**
     * Sets who can see the primary email address ({@code PATCH /user/email/visibility}).
     *
     * @param visibility the new visibility
     * @return all addresses of the account afterwards
     * @throws NullPointerException if {@code visibility} is {@code null}
     * @throws IOException if the transport fails
     */
    public List<GitHubEmail> changeEmailVisibility(final GitHubEmailVisibility visibility) throws IOException {
        return github().users()
                .emails()
                .setPrimaryEmailVisibility(
                        new GitHubSetPrimaryEmailVisibilityPayload(requireNonNull(visibility, "visibility")));
    }

    /**
     * Lists the SSH authentication keys ({@code GET /user/keys}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SshKey> sshKeys() throws IOException {
        return github().users().keys().listPublicKeys(GitHubPageRequest.byPage()).stream()
                .map(listed -> new SshKey(sdk, listed));
    }

    /**
     * Loads one SSH authentication key ({@code GET /user/keys/{key_id}}).
     *
     * @param id the key id
     * @return the key, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<SshKey> sshKey(final long id) throws IOException {
        return github().users().keys().getPublicKey(id).map(loaded -> new SshKey(sdk, loaded));
    }

    /**
     * Adds an SSH authentication key ({@code POST /user/keys}).
     *
     * @param title the title of the key
     * @param key the public key in OpenSSH format
     * @return the new key
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank
     * @throws IOException if the transport fails
     */
    public SshKey addSshKey(final String title, final String key) throws IOException {
        return new SshKey(
                sdk,
                github().users()
                        .keys()
                        .createPublicSshKey(new GitHubCreatePublicSshKeyPayload(
                                requireNotBlank(title, TITLE), requireNotBlank(key, KEY))));
    }

    /**
     * Lists the GPG keys ({@code GET /user/gpg_keys}). Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GpgKey> gpgKeys() throws IOException {
        return github().users().gpgKeys().listGpgKeys(GitHubPageRequest.byPage()).stream()
                .map(listed -> new GpgKey(sdk, listed));
    }

    /**
     * Loads one GPG key ({@code GET /user/gpg_keys/{gpg_key_id}}).
     *
     * @param id the key id
     * @return the key, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GpgKey> gpgKey(final long id) throws IOException {
        return github().users().gpgKeys().getGpgKey(id).map(loaded -> new GpgKey(sdk, loaded));
    }

    /**
     * Adds a GPG key without a name. See {@link #addGpgKey(String, String)}.
     *
     * @param armoredPublicKey the public key in ASCII armor
     * @return the new key
     * @throws NullPointerException if {@code armoredPublicKey} is {@code null}
     * @throws IllegalArgumentException if {@code armoredPublicKey} is blank
     * @throws IOException if the transport fails
     */
    public GpgKey addGpgKey(final String armoredPublicKey) throws IOException {
        return createGpgKey(null, armoredPublicKey);
    }

    /**
     * Adds a GPG key ({@code POST /user/gpg_keys}).
     *
     * @param name the name of the key
     * @param armoredPublicKey the public key in ASCII armor
     * @return the new key
     * @throws NullPointerException if {@code name} or {@code armoredPublicKey} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code armoredPublicKey} is blank
     * @throws IOException if the transport fails
     */
    public GpgKey addGpgKey(final String name, final String armoredPublicKey) throws IOException {
        return createGpgKey(requireNotBlank(name, "name"), armoredPublicKey);
    }

    /**
     * Lists the SSH signing keys ({@code GET /user/ssh_signing_keys}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SigningKey> signingKeys() throws IOException {
        return github().users().sshSigningKeys().listSshSigningKeys(GitHubPageRequest.byPage()).stream()
                .map(listed -> new SigningKey(sdk, listed));
    }

    /**
     * Loads one SSH signing key ({@code GET /user/ssh_signing_keys/{ssh_signing_key_id}}).
     *
     * @param id the key id
     * @return the key, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<SigningKey> signingKey(final long id) throws IOException {
        return github().users().sshSigningKeys().getSshSigningKey(id).map(loaded -> new SigningKey(sdk, loaded));
    }

    /**
     * Adds an SSH signing key ({@code POST /user/ssh_signing_keys}).
     *
     * @param title the title of the key
     * @param key the public key in OpenSSH format
     * @return the new key
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank
     * @throws IOException if the transport fails
     */
    public SigningKey addSigningKey(final String title, final String key) throws IOException {
        return new SigningKey(
                sdk,
                github().users()
                        .sshSigningKeys()
                        .createSshSigningKey(new GitHubCreateSshSigningKeyPayload(
                                requireNotBlank(title, TITLE), requireNotBlank(key, KEY))));
    }

    /**
     * Lists the accounts that follow this account ({@code GET /user/followers}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the followers
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> followers() throws IOException {
        return github().users().followers().listFollowers(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the accounts this account follows ({@code GET /user/following}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the followed accounts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> following() throws IOException {
        return github().users().followers().listFollowing(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Tells whether this account follows a user ({@code GET /user/following/{username}}).
     *
     * @param login the login of the user
     * @return {@code true} when the user is followed
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public boolean isFollowing(final String login) throws IOException {
        return github().users().followers().isFollowing(requireNotBlank(login, LOGIN));
    }

    /**
     * Follows a user ({@code PUT /user/following/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void follow(final String login) throws IOException {
        github().users().followers().follow(requireNotBlank(login, LOGIN));
    }

    /**
     * Stops following a user ({@code DELETE /user/following/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void unfollow(final String login) throws IOException {
        github().users().followers().unfollow(requireNotBlank(login, LOGIN));
    }

    /**
     * Lists the users this account blocked ({@code GET /user/blocks}) as GitHub returns them. Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the blocked users
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> blocked() throws IOException {
        return github().users().blocking().listBlocked(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Tells whether this account blocked a user ({@code GET /user/blocks/{username}}).
     *
     * @param login the login of the user
     * @return {@code true} when the user is blocked
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public boolean isBlocked(final String login) throws IOException {
        return github().users().blocking().isBlocked(requireNotBlank(login, LOGIN));
    }

    /**
     * Blocks a user ({@code PUT /user/blocks/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void block(final String login) throws IOException {
        github().users().blocking().block(requireNotBlank(login, LOGIN));
    }

    /**
     * Unblocks a user ({@code DELETE /user/blocks/{username}}).
     *
     * @param login the login of the user
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws IOException if the transport fails
     */
    public void unblock(final String login) throws IOException {
        github().users().blocking().unblock(requireNotBlank(login, LOGIN));
    }

    /**
     * Lists the social accounts on the profile ({@code GET /user/social_accounts}) as GitHub returns them. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the social accounts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSocialAccount> socialAccounts() throws IOException {
        return github().users().socialAccounts().listSocialAccounts(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Adds social accounts to the profile ({@code POST /user/social_accounts}).
     *
     * @param urls the URLs of the accounts, at least one
     * @return the social accounts of the profile afterwards
     * @throws NullPointerException if {@code urls} or a URL is {@code null}
     * @throws IllegalArgumentException if {@code urls} is empty or a URL is blank
     * @throws IOException if the transport fails
     */
    public List<GitHubSocialAccount> addSocialAccounts(final List<String> urls) throws IOException {
        return github().users()
                .socialAccounts()
                .addSocialAccounts(
                        new GitHubUpdateSocialAccountsPayload(Arguments.requireNotBlankValues(urls, "urls")));
    }

    /**
     * Removes social accounts from the profile ({@code DELETE /user/social_accounts}).
     *
     * @param urls the URLs of the accounts, at least one
     * @throws NullPointerException if {@code urls} or a URL is {@code null}
     * @throws IllegalArgumentException if {@code urls} is empty or a URL is blank
     * @throws IOException if the transport fails
     */
    public void removeSocialAccounts(final List<String> urls) throws IOException {
        github().users()
                .socialAccounts()
                .deleteSocialAccounts(
                        new GitHubUpdateSocialAccountsPayload(Arguments.requireNotBlankValues(urls, "urls")));
    }

    /**
     * Lists the packages of the authenticated account of one type ({@code GET /user/packages}). GitHub requires the
     * type, and its REST API for packages needs a classic token. The stream is lazy and can be used once.
     *
     * @param type the package type
     * @return the packages
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryPackage> registryPackages(final GitHubPackageType type) throws IOException {
        return RegistryPackages.forAuthenticatedUser(sdk).all(type);
    }

    /**
     * Loads one package of the authenticated account ({@code GET /user/packages/{package_type}/{package_name}}).
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
        return RegistryPackages.forAuthenticatedUser(sdk).find(type, name);
    }

    /**
     * Lists the events the account performed, including private ones, newest first ({@code GET
     * /users/{username}/events}). GitHub keeps only the last 90 days of events, at most 300, and caches the feed for a
     * few seconds, so a new event can take a moment to show. The stream is lazy and can be used once.
     *
     * <p>Loads the profile first ({@code GET /user}) to read the login.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the profile or the first page cannot be loaded
     */
    public Stream<GitHubEvent> events() throws IOException {
        return github()
                .activity()
                .events()
                .listEventsForAuthenticatedUser(profile().login())
                .stream();
    }

    /**
     * Lists the events the account received, including private ones, from what it watches and follows ({@code GET
     * /users/{username}/received_events}). GitHub keeps only the last 90 days of events, at most 300, and caches the
     * feed for a few seconds, so a new event can take a moment to show. The stream is lazy and can be used once.
     *
     * <p>Loads the profile first ({@code GET /user}) to read the login.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the profile or the first page cannot be loaded
     */
    public Stream<GitHubEvent> receivedEvents() throws IOException {
        return github().activity().events().listReceivedEventsForUser(profile().login()).stream();
    }

    /**
     * Lists the Marketplace subscriptions of the account ({@code GET /user/marketplace_purchases}) as GitHub returns
     * them. The stream is lazy and can be used once.
     *
     * @return the subscriptions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubUserMarketplacePurchase> marketplaceSubscriptions() throws IOException {
        return github().apps().marketplace().listSubscriptionsForAuthenticatedUser(GitHubPageRequest.byPage()).stream();
    }

    User apply(final GitHubUpdateAuthenticatedUserPayload payload) throws IOException {
        return new User(sdk, github().users().user().updateAuthenticatedUser(payload));
    }

    private GpgKey createGpgKey(final @Nullable String name, final String armoredPublicKey) throws IOException {
        return new GpgKey(
                sdk,
                github().users()
                        .gpgKeys()
                        .createGpgKey(new GitHubCreateGpgKeyPayload(name, requireNotBlank(armoredPublicKey, KEY))));
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
