package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAsset;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAssetBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAssetState;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleasesAssetsApi;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleaseAssetPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ReleaseAssetTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 77L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubReleaseAsset response = RepositoryContentFixtures.releaseAsset();
    private final ReleaseAsset sut = new ReleaseAsset(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAssetIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("app.zip");
            softly.assertThat(sut.label()).isEmpty();
            softly.assertThat(sut.state()).isEqualTo(GitHubReleaseAssetState.UPLOADED);
            softly.assertThat(sut.contentType()).isEqualTo("application/zip");
            softly.assertThat(sut.size()).isEqualTo(1024L);
            softly.assertThat(sut.downloadCount()).isEqualTo(3L);
            softly.assertThat(sut.downloadUrl()).endsWith("app.zip");
            softly.assertThat(sut.digest()).isEmpty();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void optionalAccessors_shouldReturnTheValues_whenTheResponseHasThem() {
        final ReleaseAsset described = new ReleaseAsset(
                repository,
                GitHubReleaseAssetBuilder.builder(response)
                        .label("Application")
                        .digest("sha256:abc")
                        .build());

        assertSoftly(softly -> {
            softly.assertThat(described.label()).contains("Application");
            softly.assertThat(described.digest()).contains("sha256:abc");
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedAsset_whenItExists() throws IOException {
        final GitHubReleaseAsset reloaded =
                GitHubReleaseAssetBuilder.builder(response).label("New").build();
        final GitHubReleasesAssetsApi assets = github.releases().assets();
        doReturn(Optional.of(reloaded)).when(assets).getReleaseAsset(OWNER, NAME, ID);

        final ReleaseAsset result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAssetIsGone() throws IOException {
        final GitHubReleasesAssetsApi assets = github.releases().assets();
        doReturn(Optional.empty()).when(assets).getReleaseAsset(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void update_shouldSendTheChangedFields_whenNameAndLabelAreSet() throws IOException {
        final GitHubReleaseAsset updated =
                GitHubReleaseAssetBuilder.builder(response).name("tool.zip").build();
        when(github.releases().assets().updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateReleaseAssetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReleaseAssetPayload.class);

        final ReleaseAsset result = sut.update().name("tool.zip").label("Tool").send();

        verify(github.releases().assets()).updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(payload.getValue().name()).isEqualTo("tool.zip");
            softly.assertThat(payload.getValue().label()).isEqualTo("Tool");
        });
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingIsSet() {
        final ReleaseAssetUpdate update = sut.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final ReleaseAssetUpdate update = sut.update();

        final Throwable thrown = catchThrowable(() -> update.name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void update_shouldThrowNullPointerException_whenLabelIsNull() {
        final ReleaseAssetUpdate update = sut.update();

        final Throwable thrown = catchThrowable(() -> update.label(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("label");
    }

    @Test
    void download_shouldWriteTheFile_whenCalledWithAPath() throws IOException {
        final Path target = Path.of("app.zip");
        when(github.releases().assets().downloadReleaseAsset(OWNER, NAME, ID, target))
                .thenReturn(target);

        final Path result = sut.download(target);

        assertThat(result).isEqualTo(target);
        verify(github.releases().assets()).downloadReleaseAsset(OWNER, NAME, ID, target);
    }

    @Test
    void download_shouldWriteTheBytes_whenCalledWithAStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] content = {1, 2, 3};
        when(github.releases().assets().downloadReleaseAsset(OWNER, NAME, ID, out))
                .thenAnswer(invocation -> {
                    out.write(content);
                    return 3L;
                });

        final long written = sut.download(out);

        verify(github.releases().assets()).downloadReleaseAsset(OWNER, NAME, ID, out);
        assertSoftly(softly -> {
            softly.assertThat(written).isEqualTo(3L);
            softly.assertThat(out.toByteArray()).containsExactly(content);
        });
    }

    @Test
    void download_shouldThrowNullPointerException_whenTargetIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.download((Path) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("target");
    }

    @Test
    void download_shouldThrowNullPointerException_whenStreamIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.download((ByteArrayOutputStream) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("out");
    }

    @Test
    void delete_shouldDeleteTheAsset_whenCalled() throws IOException {
        sut.delete();

        verify(github.releases().assets()).deleteReleaseAsset(OWNER, NAME, ID);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new ReleaseAsset(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void update_shouldSendOnlyTheName_whenOnlyTheNameIsSet() throws IOException {
        when(github.releases().assets().updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdateReleaseAssetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReleaseAssetPayload.class);

        sut.update().name("tool.zip").send();

        verify(github.releases().assets()).updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("tool.zip");
            softly.assertThat(payload.getValue().label()).isNull();
        });
    }

    @Test
    void update_shouldSendOnlyTheLabel_whenOnlyTheLabelIsSet() throws IOException {
        when(github.releases().assets().updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdateReleaseAssetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReleaseAssetPayload.class);

        sut.update().label("Tool").send();

        verify(github.releases().assets()).updateReleaseAsset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().label()).isEqualTo("Tool");
            softly.assertThat(payload.getValue().name()).isNull();
        });
    }
}
