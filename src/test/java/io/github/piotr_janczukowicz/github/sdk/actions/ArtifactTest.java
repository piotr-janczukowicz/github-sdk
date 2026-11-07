package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsArtifactsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifact;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifactBuilder;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ArtifactTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ActionsFixtures.ARTIFACT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubArtifact response = ActionsFixtures.artifact();
    private final Artifact sut = new Artifact(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenArtifactIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("report");
            softly.assertThat(sut.sizeInBytes()).isEqualTo(2048);
            softly.assertThat(sut.isExpired()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new Artifact(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Artifact(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void isExpired_shouldBeTrue_whenTheRetentionEnded() {
        final GitHubArtifact expired =
                GitHubArtifactBuilder.builder(response).expired(true).build();
        final Artifact artifact = new Artifact(repository, expired);

        final boolean result = artifact.isExpired();

        assertThat(result).isTrue();
    }

    @Test
    void refresh_shouldReturnTheReloadedArtifact_whenItExists() throws IOException {
        final GitHubArtifact reloaded =
                GitHubArtifactBuilder.builder(response).expired(true).build();
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();
        doReturn(Optional.of(reloaded)).when(artifacts).getArtifact(OWNER, NAME, ID);

        final Artifact result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheArtifactIsGone() throws IOException {
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();
        doReturn(Optional.empty()).when(artifacts).getArtifact(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void downloadUrl_shouldReturnTheRedirectLink_whenCalled() throws IOException {
        final URI link = URI.create("https://artifacts.example.com/report.zip");
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();
        doReturn(link).when(artifacts).downloadArtifact(OWNER, NAME, ID);

        final URI result = sut.downloadUrl();

        assertThat(result).isEqualTo(link);
    }

    @Test
    void delete_shouldDeleteTheArtifact_whenCalled() throws IOException {
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();

        sut.delete();

        verify(artifacts).deleteArtifact(OWNER, NAME, ID);
    }
}
