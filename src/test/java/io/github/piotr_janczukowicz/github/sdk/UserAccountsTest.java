package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubKeySimple;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubSocialAccount;
import io.github.piotr_janczukowicz.github.api.users.GitHubSshSigningKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AccountsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class UserAccountsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final User sut = userNamed("octocat");

    @Test
    void followers_shouldStreamTheFollowers_whenTheyExist() throws IOException {
        final GitHubSimpleUser follower = mock(GitHubSimpleUser.class);
        when(github.users().followers().listFollowers(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(follower));

        final List<GitHubSimpleUser> followers = sut.followers().toList();

        assertThat(followers).containsExactly(follower);
    }

    @Test
    void following_shouldStreamTheFollowedAccounts_whenTheyExist() throws IOException {
        final GitHubSimpleUser followed = mock(GitHubSimpleUser.class);
        when(github.users().followers().listFollowing(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(followed));

        final List<GitHubSimpleUser> following = sut.following().toList();

        assertThat(following).containsExactly(followed);
    }

    @Test
    void sshKeys_shouldStreamThePublicKeys_whenTheyExist() throws IOException {
        final GitHubKeySimple key = mock(GitHubKeySimple.class);
        when(github.users().keys().listPublicKeys(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(key));

        final List<GitHubKeySimple> keys = sut.sshKeys().toList();

        assertThat(keys).containsExactly(key);
    }

    @Test
    void gpgKeys_shouldStreamThePublicKeys_whenTheyExist() throws IOException {
        final GitHubGpgKey key = AccountsFixtures.gpgKey();
        when(github.users().gpgKeys().listGpgKeys(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(key));

        final List<GitHubGpgKey> keys = sut.gpgKeys().toList();

        assertThat(keys).containsExactly(key);
    }

    @Test
    void signingKeys_shouldStreamTheSigningKeys_whenTheyExist() throws IOException {
        final GitHubSshSigningKey key = AccountsFixtures.signingKey();
        when(github.users().sshSigningKeys().listSshSigningKeys(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(key));

        final List<GitHubSshSigningKey> keys = sut.signingKeys().toList();

        assertThat(keys).containsExactly(key);
    }

    @Test
    void socialAccounts_shouldStreamTheAccounts_whenTheyExist() throws IOException {
        final GitHubSocialAccount account = AccountsFixtures.socialAccount();
        when(github.users().socialAccounts().listSocialAccounts(eq("octocat"), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(account));

        final List<GitHubSocialAccount> accounts = sut.socialAccounts().toList();

        assertThat(accounts).containsExactly(account);
    }

    private User userNamed(final String login) {
        final GitHubUser response = mock(GitHubUser.class);
        when(response.login()).thenReturn(login);
        return new User(sdk, response);
    }
}
