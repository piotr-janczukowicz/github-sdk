package io.github.piotr_janczukowicz.github.sdk.deploykeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKey;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKeyBuilder;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKeysApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.NotificationFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class DeployKeyTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = NotificationFixtures.KEY_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubDeployKey response = NotificationFixtures.deployKey();
    private final DeployKey sut = new DeployKey(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenKeyIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.title()).isEqualTo("ci");
            softly.assertThat(sut.key()).startsWith("ssh-ed25519");
            softly.assertThat(sut.isReadOnly()).isTrue();
            softly.assertThat(sut.isVerified()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedKey_whenItExists() throws IOException {
        final GitHubDeployKey reloaded =
                GitHubDeployKeyBuilder.builder(response).title("renamed").build();
        final GitHubDeployKeysApi api = github.deployKeys();
        doReturn(Optional.of(reloaded)).when(api).getDeployKey(OWNER, NAME, ID);

        final DeployKey result = sut.refresh();

        assertThat(result.title()).isEqualTo("renamed");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheKeyIsGone() throws IOException {
        final GitHubDeployKeysApi api = github.deployKeys();
        doReturn(Optional.empty()).when(api).getDeployKey(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheKey_whenCalled() throws IOException {
        final GitHubDeployKeysApi api = github.deployKeys();

        sut.delete();

        verify(api).deleteDeployKey(OWNER, NAME, ID);
    }
}
