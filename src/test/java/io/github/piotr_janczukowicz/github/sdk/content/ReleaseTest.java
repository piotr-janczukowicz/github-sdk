package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseAsset;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseBuilder;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ReleaseTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 9009L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRelease response = RepositoryContentFixtures.release();
    private final Release sut = new Release(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenReleaseIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.tagName()).isEqualTo("v1.0.0");
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.isDraft()).isFalse();
            softly.assertThat(sut.isPrerelease()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void assets_shouldWrapTheListedAssets_whenTheReleaseHasThem() throws IOException {
        final GitHubReleaseAsset listed = RepositoryContentFixtures.releaseAsset();
        when(github.releases().assets().listReleaseAssets(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(listed));

        final List<ReleaseAsset> assets = sut.assets().toList();

        assertThat(assets)
                .singleElement()
                .satisfies(asset -> assertThat(asset.response()).isSameAs(listed));
    }

    @Test
    void uploadAsset_shouldSendTheFileWithTheGivenValues_whenCalledWithAllArguments() throws IOException {
        final Path file = Path.of("build/app.zip");
        final GitHubReleaseAsset uploaded = RepositoryContentFixtures.releaseAsset();
        when(github.releases()
                        .assets()
                        .uploadReleaseAsset(response.uploadUrl(), file, "application/zip", "tool.zip", "Tool"))
                .thenReturn(uploaded);

        final ReleaseAsset asset = sut.uploadAsset(file, "application/zip", "tool.zip", "Tool");

        assertThat(asset.response()).isSameAs(uploaded);
    }

    @Test
    void uploadAsset_shouldNameTheAssetAfterTheFile_whenCalledWithOnlyAFile() throws IOException {
        final Path file = Path.of("build/payload.unknownext");
        final GitHubReleaseAsset uploaded = RepositoryContentFixtures.releaseAsset();
        when(github.releases()
                        .assets()
                        .uploadReleaseAsset(
                                response.uploadUrl(), file, "application/octet-stream", "payload.unknownext", null))
                .thenReturn(uploaded);

        final ReleaseAsset asset = sut.uploadAsset(file);

        assertThat(asset.response()).isSameAs(uploaded);
    }

    @Test
    void uploadAsset_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Path file = Path.of("build/app.zip");

        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(file, "application/zip", " ", null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void uploadAsset_shouldThrowNullPointerException_whenFileIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("file");
    }

    @Test
    void name_shouldReturnTheName_whenTheResponseHasOne() {
        final Release named = new Release(
                repository, GitHubReleaseBuilder.builder(response).name("First").build());

        final Optional<String> name = named.name();

        assertThat(name).contains("First");
    }

    @Test
    void flags_shouldBeTrue_whenTheResponseSaysDraftAndPrerelease() {
        final Release flagged = new Release(
                repository,
                GitHubReleaseBuilder.builder(response)
                        .draft(true)
                        .prerelease(true)
                        .build());

        assertSoftly(softly -> {
            softly.assertThat(flagged.isDraft()).isTrue();
            softly.assertThat(flagged.isPrerelease()).isTrue();
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedRelease_whenGitHubHasIt() throws IOException {
        final GitHubRelease reloaded =
                GitHubReleaseBuilder.builder(response).name("Renamed").build();
        when(github.releases().release().getRelease(OWNER, NAME, ID)).thenReturn(Optional.of(reloaded));

        final Release refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
            softly.assertThat(sut.name()).isEmpty();
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheReleaseIsGone() throws IOException {
        when(github.releases().release().getRelease(OWNER, NAME, ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("v1.0.0");
    }

    @Test
    void delete_shouldDeleteTheRelease_whenCalled() throws IOException {
        sut.delete();

        verify(github.releases().release()).deleteRelease(OWNER, NAME, ID);
    }

    @Test
    void uploadAsset_shouldThrowIllegalArgumentException_whenContentTypeIsBlank() {
        final Path file = Path.of("build/app.zip");

        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(file, " ", "app.zip", null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("contentType");
    }

    @Test
    void uploadAsset_shouldThrowNullPointerException_whenContentTypeIsNull() {
        final Path file = Path.of("build/app.zip");

        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(file, null, "app.zip", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("contentType");
    }

    @Test
    void uploadAsset_shouldThrowNullPointerException_whenNameIsNull() {
        final Path file = Path.of("build/app.zip");

        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(file, "application/zip", null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void uploadAsset_shouldThrowNullPointerException_whenFileIsNullInTheLongForm() {
        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(null, "application/zip", "app.zip", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("file");
    }

    @Test
    void uploadAsset_shouldThrowIllegalArgumentException_whenTheFileHasNoFileName() {
        final Path root = Path.of("/").toAbsolutePath().getRoot();

        final Throwable thrown = catchThrowable(() -> sut.uploadAsset(root));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file name");
    }
}
