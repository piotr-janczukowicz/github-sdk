package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubCreateReleasePayload;
import io.github.piotr_janczukowicz.github.api.releases.GitHubCreateReleasePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryTag;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryReleasesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRelease record = RepositoryContentFixtures.release();

    @Test
    void release_shouldReturnTheRelease_whenGitHubHasTheTag() throws IOException {
        when(github.releases().release().getReleaseByTag(OWNER, NAME, "v1.0.0")).thenReturn(Optional.of(record));

        final Optional<Release> release = sut.release("v1.0.0");

        assertThat(release).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(release.orElseThrow().record()).isSameAs(record);
            softly.assertThat(release.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void release_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.releases().release().getReleaseByTag(OWNER, NAME, "v9")).thenReturn(Optional.empty());

        final Optional<Release> release = sut.release("v9");

        assertThat(release).isEmpty();
    }

    @Test
    void release_shouldThrowIllegalArgumentException_whenTagIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.release(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tag");
    }

    @Test
    void latestRelease_shouldReturnTheRelease_whenThereIsOne() throws IOException {
        when(github.releases().release().getLatestRelease(OWNER, NAME)).thenReturn(Optional.of(record));

        final Optional<Release> release = sut.latestRelease();

        assertThat(release).map(Release::record).containsSame(record);
    }

    @Test
    void latestRelease_shouldReturnEmpty_whenThereIsNone() throws IOException {
        when(github.releases().release().getLatestRelease(OWNER, NAME)).thenReturn(Optional.empty());

        final Optional<Release> release = sut.latestRelease();

        assertThat(release).isEmpty();
    }

    @Test
    void releases_shouldWrapEachRelease_whenTheRepositoryHasReleases() throws IOException {
        when(github.releases().release().listReleases(OWNER, NAME).stream()).thenReturn(Stream.of(record));

        final List<Release> releases = sut.releases().toList();

        assertThat(releases).extracting(Release::record).containsExactly(record);
    }

    @Test
    void createRelease_shouldSendTheTagAndReturnTheRelease_whenCalledWithATag() throws IOException {
        when(github.releases().release().createRelease(eq(OWNER), eq(NAME), any(GitHubCreateReleasePayload.class)))
                .thenReturn(record);
        final ArgumentCaptor<GitHubCreateReleasePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateReleasePayload.class);

        final Release release = sut.createRelease("v1.0.0");

        verify(github.releases().release()).createRelease(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().tagName()).isEqualTo("v1.0.0");
            softly.assertThat(payload.getValue().draft()).isNull();
            softly.assertThat(release.record()).isSameAs(record);
        });
    }

    @Test
    void createRelease_shouldSendThePayload_whenCalledWithAPayload() throws IOException {
        final GitHubCreateReleasePayload payload = GitHubCreateReleasePayloadBuilder.builder()
                .tagName("v1.0.0")
                .draft(true)
                .build();
        when(github.releases().release().createRelease(OWNER, NAME, payload)).thenReturn(record);

        final Release release = sut.createRelease(payload);

        assertThat(release.record()).isSameAs(record);
    }

    @Test
    void createRelease_shouldThrowIllegalArgumentException_whenTagIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createRelease(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tagName");
    }

    @Test
    void createRelease_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createRelease((GitHubCreateReleasePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void tags_shouldStreamTheTags_whenTheRepositoryHasTags() throws IOException {
        final GitHubRepositoryTag tag = mock(GitHubRepositoryTag.class);
        when(github.repositories().repository().listTags(OWNER, NAME).stream()).thenReturn(Stream.of(tag));

        final List<GitHubRepositoryTag> tags = sut.tags().toList();

        assertThat(tags).containsExactly(tag);
    }
}
