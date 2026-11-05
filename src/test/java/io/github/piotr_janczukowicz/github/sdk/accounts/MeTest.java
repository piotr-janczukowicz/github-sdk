package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreateGpgKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreatePublicSshKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubCreateSshSigningKeyPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubEmail;
import io.github.piotr_janczukowicz.github.api.users.GitHubEmailVisibility;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubSetPrimaryEmailVisibilityPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubSocialAccount;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateEmailsPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateSocialAccountsPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserView;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersBlockingApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersEmailsApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersFollowersApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersGpgKeysApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersKeysApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersSocialAccountsApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersSshSigningKeysApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.User;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AccountsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MeTest {

    private static final long ID = AccountsFixtures.KEY_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Me sut = sdk.me();

    @Test
    void profile_shouldReturnTheAuthenticatedUser_whenCalled() throws IOException {
        final GitHubUserView view = mock(GitHubUser.class);
        final GitHubUserApi api = github.users().user();
        doReturn(view).when(api).getAuthenticatedUser();

        final User profile = sut.profile();

        assertThat(profile.response()).isSameAs(view);
    }

    @Test
    void emails_shouldStreamTheEmails_whenTheyExist() throws IOException {
        final GitHubEmail email = AccountsFixtures.email();
        when(github.users().emails().listEmails(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(email));

        final List<GitHubEmail> emails = sut.emails().toList();

        assertThat(emails).containsExactly(email);
    }

    @Test
    void addEmail_shouldSendTheAddress_whenCalled() throws IOException {
        final GitHubUsersEmailsApi api = github.users().emails();
        doReturn(List.of(AccountsFixtures.email())).when(api).addEmails(any(GitHubUpdateEmailsPayload.class));
        final ArgumentCaptor<GitHubUpdateEmailsPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateEmailsPayload.class);

        final List<GitHubEmail> emails = sut.addEmail("new@example.com");

        verify(api).addEmails(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().emails()).containsExactly("new@example.com");
            softly.assertThat(emails).hasSize(1);
        });
    }

    @Test
    void addEmail_shouldThrowIllegalArgumentException_whenAddressIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addEmail(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("address");
    }

    @Test
    void removeEmail_shouldSendTheAddress_whenCalled() throws IOException {
        final GitHubUsersEmailsApi api = github.users().emails();

        sut.removeEmail("old@example.com");

        verify(api).deleteEmails(new GitHubUpdateEmailsPayload(List.of("old@example.com")));
    }

    @Test
    void removeEmail_shouldThrowIllegalArgumentException_whenAddressIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.removeEmail(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("address");
    }

    @Test
    void changeEmailVisibility_shouldSendTheVisibility_whenCalled() throws IOException {
        final GitHubUsersEmailsApi api = github.users().emails();
        doReturn(List.of(AccountsFixtures.email()))
                .when(api)
                .setPrimaryEmailVisibility(any(GitHubSetPrimaryEmailVisibilityPayload.class));
        final ArgumentCaptor<GitHubSetPrimaryEmailVisibilityPayload> payload =
                ArgumentCaptor.forClass(GitHubSetPrimaryEmailVisibilityPayload.class);

        final List<GitHubEmail> emails = sut.changeEmailVisibility(GitHubEmailVisibility.PUBLIC);

        verify(api).setPrimaryEmailVisibility(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().visibility()).isEqualTo(GitHubEmailVisibility.PUBLIC);
            softly.assertThat(emails).hasSize(1);
        });
    }

    @Test
    void changeEmailVisibility_shouldThrowNullPointerException_whenVisibilityIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.changeEmailVisibility(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("visibility");
    }

    @Test
    void sshKeys_shouldStreamTheKeys_whenTheyExist() throws IOException {
        when(github.users().keys().listPublicKeys(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(AccountsFixtures.sshKey()));

        final List<SshKey> keys = sut.sshKeys().toList();

        assertThat(keys).singleElement().satisfies(key -> assertThat(key.id()).isEqualTo(ID));
    }

    @Test
    void sshKey_shouldReturnTheKey_whenGitHubHasIt() throws IOException {
        final GitHubUsersKeysApi api = github.users().keys();
        doReturn(Optional.of(AccountsFixtures.sshKey())).when(api).getPublicKey(ID);

        final Optional<SshKey> found = sut.sshKey(ID);

        assertThat(found).isPresent();
    }

    @Test
    void sshKey_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubUsersKeysApi api = github.users().keys();
        doReturn(Optional.empty()).when(api).getPublicKey(1L);

        final Optional<SshKey> found = sut.sshKey(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void addSshKey_shouldSendTitleAndKey_whenCalled() throws IOException {
        final GitHubUsersKeysApi api = github.users().keys();
        doReturn(AccountsFixtures.sshKey())
                .when(api)
                .createPublicSshKey(new GitHubCreatePublicSshKeyPayload("laptop", "ssh-ed25519 AAAA"));

        final SshKey key = sut.addSshKey("laptop", "ssh-ed25519 AAAA");

        assertThat(key.id()).isEqualTo(ID);
    }

    @Test
    void addSshKey_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSshKey(" ", "ssh-ed25519 AAAA"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void addSshKey_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSshKey("laptop", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }

    @Test
    void gpgKeys_shouldStreamTheKeys_whenTheyExist() throws IOException {
        final GitHubGpgKey listed = AccountsFixtures.gpgKey();
        when(github.users().gpgKeys().listGpgKeys(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<GpgKey> keys = sut.gpgKeys().toList();

        assertThat(keys).singleElement().satisfies(key -> assertThat(key.id()).isEqualTo(ID));
    }

    @Test
    void gpgKey_shouldReturnTheKey_whenGitHubHasIt() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(Optional.of(AccountsFixtures.gpgKey())).when(api).getGpgKey(ID);

        final Optional<GpgKey> found = sut.gpgKey(ID);

        assertThat(found).isPresent();
    }

    @Test
    void gpgKey_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(Optional.empty()).when(api).getGpgKey(1L);

        final Optional<GpgKey> found = sut.gpgKey(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void addGpgKey_shouldSendOnlyTheKey_whenNoNameIsGiven() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(AccountsFixtures.gpgKey()).when(api).createGpgKey(any(GitHubCreateGpgKeyPayload.class));
        final ArgumentCaptor<GitHubCreateGpgKeyPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateGpgKeyPayload.class);

        sut.addGpgKey("-----BEGIN PGP PUBLIC KEY BLOCK-----");

        verify(api).createGpgKey(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(payload.getValue().armoredPublicKey()).startsWith("-----BEGIN");
        });
    }

    @Test
    void addGpgKey_shouldSendTheName_whenOneIsGiven() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(AccountsFixtures.gpgKey()).when(api).createGpgKey(any(GitHubCreateGpgKeyPayload.class));
        final ArgumentCaptor<GitHubCreateGpgKeyPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateGpgKeyPayload.class);

        sut.addGpgKey("work", "-----BEGIN PGP PUBLIC KEY BLOCK-----");

        verify(api).createGpgKey(payload.capture());
        assertThat(payload.getValue().name()).isEqualTo("work");
    }

    @Test
    void addGpgKey_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addGpgKey(" ", "-----BEGIN"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void addGpgKey_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addGpgKey(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }

    @Test
    void signingKeys_shouldStreamTheKeys_whenTheyExist() throws IOException {
        when(github.users().sshSigningKeys().listSshSigningKeys(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(AccountsFixtures.signingKey()));

        final List<SigningKey> keys = sut.signingKeys().toList();

        assertThat(keys).singleElement().satisfies(key -> assertThat(key.id()).isEqualTo(ID));
    }

    @Test
    void signingKey_shouldReturnTheKey_whenGitHubHasIt() throws IOException {
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();
        doReturn(Optional.of(AccountsFixtures.signingKey())).when(api).getSshSigningKey(ID);

        final Optional<SigningKey> found = sut.signingKey(ID);

        assertThat(found).isPresent();
    }

    @Test
    void signingKey_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();
        doReturn(Optional.empty()).when(api).getSshSigningKey(1L);

        final Optional<SigningKey> found = sut.signingKey(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void addSigningKey_shouldSendTitleAndKey_whenCalled() throws IOException {
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();
        doReturn(AccountsFixtures.signingKey())
                .when(api)
                .createSshSigningKey(new GitHubCreateSshSigningKeyPayload("signing", "ssh-ed25519 AAAA"));

        final SigningKey key = sut.addSigningKey("signing", "ssh-ed25519 AAAA");

        assertThat(key.id()).isEqualTo(ID);
    }

    @Test
    void addSigningKey_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSigningKey(" ", "ssh-ed25519 AAAA"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void followers_shouldStreamTheFollowers_whenTheyExist() throws IOException {
        final GitHubSimpleUser follower = mock(GitHubSimpleUser.class);
        when(github.users().followers().listFollowers(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(follower));

        final List<GitHubSimpleUser> followers = sut.followers().toList();

        assertThat(followers).containsExactly(follower);
    }

    @Test
    void following_shouldStreamTheFollowedAccounts_whenTheyExist() throws IOException {
        final GitHubSimpleUser followed = mock(GitHubSimpleUser.class);
        when(github.users().followers().listFollowing(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(followed));

        final List<GitHubSimpleUser> following = sut.following().toList();

        assertThat(following).containsExactly(followed);
    }

    @Test
    void isFollowing_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        final GitHubUsersFollowersApi api = github.users().followers();
        doReturn(true).when(api).isFollowing("hubot");

        final boolean isFollowing = sut.isFollowing("hubot");

        assertThat(isFollowing).isTrue();
    }

    @Test
    void follow_shouldFollowTheUser_whenCalled() throws IOException {
        final GitHubUsersFollowersApi api = github.users().followers();

        sut.follow("hubot");

        verify(api).follow("hubot");
    }

    @Test
    void unfollow_shouldUnfollowTheUser_whenCalled() throws IOException {
        final GitHubUsersFollowersApi api = github.users().followers();

        sut.unfollow("hubot");

        verify(api).unfollow("hubot");
    }

    @Test
    void follow_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.follow(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void blocked_shouldStreamTheBlockedUsers_whenTheyExist() throws IOException {
        final GitHubSimpleUser blocked = mock(GitHubSimpleUser.class);
        when(github.users().blocking().listBlocked(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(blocked));

        final List<GitHubSimpleUser> users = sut.blocked().toList();

        assertThat(users).containsExactly(blocked);
    }

    @Test
    void isBlocked_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        final GitHubUsersBlockingApi api = github.users().blocking();
        doReturn(true).when(api).isBlocked("troll");

        final boolean isBlocked = sut.isBlocked("troll");

        assertThat(isBlocked).isTrue();
    }

    @Test
    void block_shouldBlockTheUser_whenCalled() throws IOException {
        final GitHubUsersBlockingApi api = github.users().blocking();

        sut.block("troll");

        verify(api).block("troll");
    }

    @Test
    void unblock_shouldUnblockTheUser_whenCalled() throws IOException {
        final GitHubUsersBlockingApi api = github.users().blocking();

        sut.unblock("troll");

        verify(api).unblock("troll");
    }

    @Test
    void socialAccounts_shouldStreamTheAccounts_whenTheyExist() throws IOException {
        final GitHubSocialAccount account = AccountsFixtures.socialAccount();
        when(github.users().socialAccounts().listSocialAccounts(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(account));

        final List<GitHubSocialAccount> accounts = sut.socialAccounts().toList();

        assertThat(accounts).containsExactly(account);
    }

    @Test
    void addSocialAccounts_shouldSendTheUrls_whenCalled() throws IOException {
        final GitHubUsersSocialAccountsApi api = github.users().socialAccounts();
        doReturn(List.of(AccountsFixtures.socialAccount()))
                .when(api)
                .addSocialAccounts(new GitHubUpdateSocialAccountsPayload(List.of("https://example.social/@octocat")));

        final List<GitHubSocialAccount> accounts = sut.addSocialAccounts(List.of("https://example.social/@octocat"));

        assertThat(accounts).hasSize(1);
    }

    @Test
    void removeSocialAccounts_shouldSendTheUrls_whenCalled() throws IOException {
        final GitHubUsersSocialAccountsApi api = github.users().socialAccounts();

        sut.removeSocialAccounts(List.of("https://example.social/@octocat"));

        verify(api)
                .deleteSocialAccounts(
                        new GitHubUpdateSocialAccountsPayload(List.of("https://example.social/@octocat")));
    }

    @Test
    void addSocialAccounts_shouldThrowNullPointerException_whenUrlsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.addSocialAccounts(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("urls");
    }

    @Test
    void removeSocialAccounts_shouldThrowNullPointerException_whenUrlsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.removeSocialAccounts(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("urls");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new Me(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void addSocialAccounts_shouldThrowIllegalArgumentException_whenTheListIsEmptyOrBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSocialAccounts(List.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("urls");
    }

    @Test
    void removeSocialAccounts_shouldThrowIllegalArgumentException_whenAUrlIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.removeSocialAccounts(List.of(" ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("urls");
    }
}
