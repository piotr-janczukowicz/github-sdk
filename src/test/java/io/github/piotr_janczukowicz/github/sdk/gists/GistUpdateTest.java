package io.github.piotr_janczukowicz.github.sdk.gists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistApi;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.api.gists.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GistFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GistUpdateTest {

    private static final String ID = GistFixtures.ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Gist gist = new Gist(GitHubSdk.of(github), GistFixtures.gist());

    @Test
    void send_shouldSendOnlyTheDescription_whenOnlyTheDescriptionIsSet() throws IOException {
        final GitHubGistSimple updated = GistFixtures.gist();
        final GitHubGistApi api = github.gists().gist();
        doReturn(updated).when(api).updateGist(eq(ID), any(GitHubUpdatePayload.class));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Gist result = gist.update().description("New").send();

        verify(api).updateGist(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("New");
            softly.assertThat(payload.getValue().files()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
        });
    }

    @Test
    void send_shouldSendWritesRenamesAndDeletions_whenFilesAreChosen() throws IOException {
        final GitHubGistApi api = github.gists().gist();
        doReturn(GistFixtures.gist()).when(api).updateGist(eq(ID), any(GitHubUpdatePayload.class));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        gist.update()
                .write("a.txt", "new text")
                .rename("b.txt", "c.txt")
                .deleteFile("d.txt")
                .send();

        verify(api).updateGist(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(payload.getValue().files()).containsOnlyKeys("a.txt", "b.txt", "d.txt");
            softly.assertThat(payload.getValue().files().get("a.txt").content()).isEqualTo("new text");
            softly.assertThat(payload.getValue().files().get("b.txt").filename())
                    .isEqualTo("c.txt");
            softly.assertThat(payload.getValue().files().get("b.txt").content()).isNull();
            softly.assertThat(payload.getValue().files().get("d.txt")).isNull();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingIsSet() {
        final GistUpdate update = gist.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing to update");
        verifyNoInteractions(github);
    }

    @Test
    void description_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final GistUpdate update = gist.update();

        final Throwable thrown = catchThrowable(() -> update.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void write_shouldThrowNullPointerException_whenContentIsNull() {
        final GistUpdate update = gist.update();

        final Throwable thrown = catchThrowable(() -> update.write("a.txt", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @Test
    void rename_shouldThrowIllegalArgumentException_whenNewNameIsBlank() {
        final GistUpdate update = gist.update();

        final Throwable thrown = catchThrowable(() -> update.rename("a.txt", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("newName");
    }

    @Test
    void deleteFile_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final GistUpdate update = gist.update();

        final Throwable thrown = catchThrowable(() -> update.deleteFile(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }
}
