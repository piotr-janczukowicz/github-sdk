package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ReleaseTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 9009L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRelease record = RepositoryContentFixtures.release();
    private final Release sut = new Release(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenReleaseIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.tagName()).isEqualTo("v1.0.0");
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.isDraft()).isFalse();
            softly.assertThat(sut.isPrerelease()).isFalse();
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void name_shouldReturnTheName_whenTheRecordHasOne() {
        final Release named = new Release(
                repository, GitHubReleaseBuilder.builder(record).name("First").build());

        final Optional<String> name = named.name();

        assertThat(name).contains("First");
    }

    @Test
    void flags_shouldBeTrue_whenTheRecordSaysDraftAndPrerelease() {
        final Release flagged = new Release(
                repository,
                GitHubReleaseBuilder.builder(record)
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
                GitHubReleaseBuilder.builder(record).name("Renamed").build();
        when(github.releases().release().getRelease(OWNER, NAME, ID)).thenReturn(Optional.of(reloaded));

        final Release refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
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
}
