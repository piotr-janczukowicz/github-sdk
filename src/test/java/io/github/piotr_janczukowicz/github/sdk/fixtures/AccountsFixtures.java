package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.users.GitHubEmail;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubSocialAccount;
import io.github.piotr_janczukowicz.github.api.users.GitHubSshSigningKey;
import java.time.Instant;

public final class AccountsFixtures {

    public static final long KEY_ID = 41L;

    private static final Instant MOMENT = Instant.parse("2026-11-05T10:00:00Z");

    private AccountsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubKey sshKey() {
        return new GitHubKey(
                "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExample",
                KEY_ID,
                "https://api.github.com/user/keys/41",
                "laptop",
                MOMENT,
                true,
                false,
                MOMENT);
    }

    public static GitHubGpgKey gpgKey() {
        final GitHubGpgKey key = mock(GitHubGpgKey.class);
        lenient().when(key.id()).thenReturn(KEY_ID);
        lenient().when(key.name()).thenReturn("work");
        lenient().when(key.keyId()).thenReturn("3262EFF25BA0D270");
        lenient().when(key.canSign()).thenReturn(true);
        return key;
    }

    public static GitHubGpgKey gpgKeyWithoutName() {
        final GitHubGpgKey key = mock(GitHubGpgKey.class);
        lenient().when(key.id()).thenReturn(KEY_ID);
        lenient().when(key.name()).thenReturn(null);
        return key;
    }

    public static GitHubSshSigningKey signingKey() {
        return new GitHubSshSigningKey("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExample", KEY_ID, "signing", MOMENT);
    }

    public static GitHubEmail email() {
        return new GitHubEmail("octocat@example.com", true, true, "private");
    }

    public static GitHubSocialAccount socialAccount() {
        return new GitHubSocialAccount("mastodon", "https://example.social/@octocat");
    }
}
