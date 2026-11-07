package io.github.piotr_janczukowicz.github.sdk.gists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GistFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class GistDraftTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void send_shouldCreateASecretGistWithTheFiles_whenOnlyFilesAreAdded() throws IOException {
        final GitHubGistSimple created = GistFixtures.gist();
        final GitHubGistApi api = github.gists().gist();
        final AtomicReference<GitHubCreatePayload> payload = new AtomicReference<>();
        doAnswer(invocation -> {
                    payload.set(invocation.getArgument(0));
                    return created;
                })
                .when(api)
                .createGist(any(GitHubCreatePayload.class));

        final Gist gist =
                sdk.createGist().file("a.txt", "one").file("b.txt", "two").send();

        assertSoftly(softly -> {
            softly.assertThat(payload.get().files()).containsOnlyKeys("a.txt", "b.txt");
            softly.assertThat(payload.get().files().get("a.txt").content()).isEqualTo("one");
            softly.assertThat(payload.get().isPublic()).isFalse();
            softly.assertThat(payload.get().description()).isNull();
            softly.assertThat(gist.response()).isSameAs(created);
        });
    }

    @Test
    void send_shouldSendTheDescriptionAndListing_whenTheyAreSet() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        final AtomicReference<GitHubCreatePayload> payload = new AtomicReference<>();
        doAnswer(invocation -> {
                    payload.set(invocation.getArgument(0));
                    return GistFixtures.gist();
                })
                .when(api)
                .createGist(any(GitHubCreatePayload.class));

        sdk.createGist().description("Notes").listed(true).file("a.txt", "one").send();

        assertSoftly(softly -> {
            softly.assertThat(payload.get().description()).isEqualTo("Notes");
            softly.assertThat(payload.get().isPublic()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNoFileWasAdded() {
        final GistDraft draft = sdk.createGist();

        final Throwable thrown = catchThrowable(draft::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("at least one file");
    }

    @Test
    void description_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final GistDraft draft = sdk.createGist();

        final Throwable thrown = catchThrowable(() -> draft.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void file_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final GistDraft draft = sdk.createGist();

        final Throwable thrown = catchThrowable(() -> draft.file(" ", "one"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void file_shouldThrowNullPointerException_whenContentIsNull() {
        final GistDraft draft = sdk.createGist();

        final Throwable thrown = catchThrowable(() -> draft.file("a.txt", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @Test
    void file_shouldThrowIllegalArgumentException_whenContentIsEmpty() {
        final GistDraft draft = sdk.createGist();

        final Throwable thrown = catchThrowable(() -> draft.file("a.txt", ""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("content");
    }
}
