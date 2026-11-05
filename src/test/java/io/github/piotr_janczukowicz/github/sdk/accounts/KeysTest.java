package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubGpgKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubSshSigningKey;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersGpgKeysApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersKeysApi;
import io.github.piotr_janczukowicz.github.api.users.GitHubUsersSshSigningKeysApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AccountsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class KeysTest {

    private static final long ID = AccountsFixtures.KEY_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void sshKey_shouldExposeTheResponse_whenKeyIsLoaded() {
        final GitHubKey response = AccountsFixtures.sshKey();
        final SshKey sut = new SshKey(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.title()).isEqualTo("laptop");
            softly.assertThat(sut.key()).startsWith("ssh-ed25519");
            softly.assertThat(sut.isVerified()).isTrue();
            softly.assertThat(sut.isReadOnly()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void sshKey_shouldReturnTheReloadedKey_whenItExists() throws IOException {
        final GitHubKey reloaded = AccountsFixtures.sshKey();
        final GitHubUsersKeysApi api = github.users().keys();
        doReturn(Optional.of(reloaded)).when(api).getPublicKey(ID);

        final SshKey result = new SshKey(sdk, AccountsFixtures.sshKey()).refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void sshKey_shouldThrowIllegalStateException_whenTheKeyIsGone() throws IOException {
        final GitHubUsersKeysApi api = github.users().keys();
        doReturn(Optional.empty()).when(api).getPublicKey(ID);
        final SshKey sut = new SshKey(sdk, AccountsFixtures.sshKey());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void sshKey_shouldDeleteTheKey_whenCalled() throws IOException {
        final GitHubUsersKeysApi api = github.users().keys();

        new SshKey(sdk, AccountsFixtures.sshKey()).delete();

        verify(api).deletePublicKey(ID);
    }

    @Test
    void gpgKey_shouldExposeTheResponse_whenKeyIsLoaded() {
        final GitHubGpgKey response = AccountsFixtures.gpgKey();
        final GpgKey sut = new GpgKey(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).hasValue("work");
            softly.assertThat(sut.keyId()).isEqualTo("3262EFF25BA0D270");
            softly.assertThat(sut.canSign()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void gpgKey_shouldHaveNoName_whenGitHubReportsNone() {
        final GpgKey sut = new GpgKey(sdk, AccountsFixtures.gpgKeyWithoutName());

        assertThat(sut.name()).isEmpty();
    }

    @Test
    void gpgKey_shouldReturnTheReloadedKey_whenItExists() throws IOException {
        final GitHubGpgKey reloaded = AccountsFixtures.gpgKey();
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(Optional.of(reloaded)).when(api).getGpgKey(ID);

        final GpgKey result = new GpgKey(sdk, AccountsFixtures.gpgKey()).refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void gpgKey_shouldThrowIllegalStateException_whenTheKeyIsGone() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();
        doReturn(Optional.empty()).when(api).getGpgKey(ID);
        final GpgKey sut = new GpgKey(sdk, AccountsFixtures.gpgKey());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void gpgKey_shouldDeleteTheKey_whenCalled() throws IOException {
        final GitHubUsersGpgKeysApi api = github.users().gpgKeys();

        new GpgKey(sdk, AccountsFixtures.gpgKey()).delete();

        verify(api).deleteGpgKey(ID);
    }

    @Test
    void signingKey_shouldExposeTheResponse_whenKeyIsLoaded() {
        final GitHubSshSigningKey response = AccountsFixtures.signingKey();
        final SigningKey sut = new SigningKey(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.title()).isEqualTo("signing");
            softly.assertThat(sut.key()).startsWith("ssh-ed25519");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void signingKey_shouldReturnTheReloadedKey_whenItExists() throws IOException {
        final GitHubSshSigningKey reloaded = AccountsFixtures.signingKey();
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();
        doReturn(Optional.of(reloaded)).when(api).getSshSigningKey(ID);

        final SigningKey result = new SigningKey(sdk, AccountsFixtures.signingKey()).refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void signingKey_shouldThrowIllegalStateException_whenTheKeyIsGone() throws IOException {
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();
        doReturn(Optional.empty()).when(api).getSshSigningKey(ID);
        final SigningKey sut = new SigningKey(sdk, AccountsFixtures.signingKey());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void signingKey_shouldDeleteTheKey_whenCalled() throws IOException {
        final GitHubUsersSshSigningKeysApi api = github.users().sshSigningKeys();

        new SigningKey(sdk, AccountsFixtures.signingKey()).delete();

        verify(api).deleteSshSigningKey(ID);
    }
}
