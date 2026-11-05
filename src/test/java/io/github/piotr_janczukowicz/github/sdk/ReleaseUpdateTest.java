package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubUpdateReleasePayload;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ReleaseUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 9009L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubRelease record = RepositoryContentFixtures.release();
    private final Release release = new Release(GitHubSdk.of(github).repository(OWNER, NAME), record);

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheResponse_whenNameAndBodyAreSet() throws IOException {
        final GitHubRelease updated =
                GitHubReleaseBuilder.builder(record).name("First").build();
        when(github.releases()
                        .release()
                        .updateRelease(eq(OWNER), eq(NAME), eq(ID), any(GitHubUpdateReleasePayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateReleasePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReleasePayload.class);

        final Release result = release.update().name("First").body("Notes").send();

        verify(github.releases().release()).updateRelease(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("First");
            softly.assertThat(payload.getValue().body()).isEqualTo("Notes");
            softly.assertThat(payload.getValue().tagName()).isNull();
            softly.assertThat(payload.getValue().draft()).isNull();
            softly.assertThat(payload.getValue().prerelease()).isNull();
            softly.assertThat(result.record()).isSameAs(updated);
            softly.assertThat(release.name()).isEmpty();
        });
    }

    @Test
    void send_shouldSendTheTagAndFlags_whenTheyAreSet() throws IOException {
        when(github.releases()
                        .release()
                        .updateRelease(eq(OWNER), eq(NAME), eq(ID), any(GitHubUpdateReleasePayload.class)))
                .thenReturn(record);
        final ArgumentCaptor<GitHubUpdateReleasePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReleasePayload.class);

        release.update().tagName("v1.0.1").draft(false).prerelease(true).send();

        verify(github.releases().release()).updateRelease(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().tagName()).isEqualTo("v1.0.1");
            softly.assertThat(payload.getValue().draft()).isFalse();
            softly.assertThat(payload.getValue().prerelease()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final ReleaseUpdate update = release.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.releases().release());
    }

    @Test
    void tagName_shouldThrowNullPointerException_whenTagNameIsNull() {
        final ReleaseUpdate update = release.update();

        final Throwable thrown = catchThrowable(() -> update.tagName(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("tagName");
    }

    @Test
    void name_shouldThrowNullPointerException_whenNameIsNull() {
        final ReleaseUpdate update = release.update();

        final Throwable thrown = catchThrowable(() -> update.name(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void body_shouldThrowNullPointerException_whenBodyIsNull() {
        final ReleaseUpdate update = release.update();

        final Throwable thrown = catchThrowable(() -> update.body(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }
}
