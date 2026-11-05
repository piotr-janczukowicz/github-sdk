package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDeleteFilePayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryFileTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);

    private RepositoryFile fileOf(final byte[] content) {
        final String encoded = Base64.getMimeEncoder(4, new byte[] {'\n'}).encodeToString(content);
        return new RepositoryFile(repository, RepositoryContentFixtures.contentFile("base64", encoded), null);
    }

    @Test
    void accessors_shouldExposeTheResponse_whenFileIsLoaded() {
        final GitHubContentFile response = RepositoryContentFixtures.contentFile("base64", "SGVsbG8=");
        final RepositoryFile sut = new RepositoryFile(repository, response, null);

        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.path()).isEqualTo("docs/README.md");
            softly.assertThat(sut.sha()).isEqualTo("blob123");
            softly.assertThat(sut.size()).isEqualTo(5);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void bytes_shouldReturnTheExactContent_whenTheFileIsBinaryAndWrappedInLines() {
        final byte[] binary = {0, 1, 2, (byte) 0xFF, (byte) 0xFE, 10, 13, 127};
        final RepositoryFile sut = fileOf(binary);

        final byte[] bytes = sut.bytes();

        assertThat(bytes).isEqualTo(binary);
    }

    @Test
    void bytes_shouldReturnAnEmptyArray_whenTheFileIsEmpty() {
        final RepositoryFile sut = fileOf(new byte[0]);

        final byte[] bytes = sut.bytes();

        assertThat(bytes).isEmpty();
    }

    @Test
    void bytes_shouldReturnACopy_whenCalledTwice() {
        final RepositoryFile sut = fileOf("Hello".getBytes(StandardCharsets.UTF_8));
        sut.bytes()[0] = 0;

        final byte[] bytes = sut.bytes();

        assertThat(bytes).isEqualTo("Hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void bytes_shouldThrowIllegalStateException_whenGitHubSendsNoContent() {
        final RepositoryFile sut =
                new RepositoryFile(repository, RepositoryContentFixtures.contentFile("none", ""), null);

        final Throwable thrown = catchThrowable(sut::bytes);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("none");
    }

    @Test
    void bytes_shouldThrowIllegalStateException_whenTheContentIsNotValidBase64() {
        final RepositoryFile sut =
                new RepositoryFile(repository, RepositoryContentFixtures.contentFile("base64", "SGVs*bG8="), null);

        final Throwable thrown = catchThrowable(sut::bytes);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("base64");
    }

    @Test
    void text_shouldDecodeUtf8_whenTheContentIsText() {
        final RepositoryFile sut = fileOf("héllo wörld".getBytes(StandardCharsets.UTF_8));

        final String text = sut.text();

        assertThat(text).isEqualTo("héllo wörld");
    }

    @Test
    void text_shouldThrowIllegalStateException_whenTheContentIsNotUtf8() {
        final RepositoryFile sut = fileOf(new byte[] {(byte) 0xC3, 0x28});

        final Throwable thrown = catchThrowable(sut::text);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("UTF-8");
    }

    @Test
    void delete_shouldSendTheShaAndMessage_whenCalledWithoutABranch() throws IOException {
        final RepositoryFile sut = fileOf("Hello".getBytes(StandardCharsets.UTF_8));
        final ArgumentCaptor<GitHubDeleteFilePayload> payload = ArgumentCaptor.forClass(GitHubDeleteFilePayload.class);

        sut.delete("Remove the file");

        verify(github.repositories().contents())
                .deleteFile(eq(OWNER), eq(NAME), eq("docs/README.md"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().message()).isEqualTo("Remove the file");
            softly.assertThat(payload.getValue().sha()).isEqualTo("blob123");
            softly.assertThat(payload.getValue().branch()).isNull();
        });
    }

    @Test
    void delete_shouldSendTheBranch_whenCalledWithABranch() throws IOException {
        final RepositoryFile sut = fileOf("Hello".getBytes(StandardCharsets.UTF_8));
        final ArgumentCaptor<GitHubDeleteFilePayload> payload = ArgumentCaptor.forClass(GitHubDeleteFilePayload.class);

        sut.delete("Remove the file", "feature");

        verify(github.repositories().contents())
                .deleteFile(eq(OWNER), eq(NAME), eq("docs/README.md"), payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo("feature");
    }

    @Test
    void delete_shouldSendTheRefAsTheBranch_whenTheFileWasReadAtARef() throws IOException {
        final RepositoryFile sut =
                new RepositoryFile(repository, RepositoryContentFixtures.contentFile("base64", "SGVsbG8="), "feature");
        final ArgumentCaptor<GitHubDeleteFilePayload> payload = ArgumentCaptor.forClass(GitHubDeleteFilePayload.class);

        sut.delete("Remove the file");

        verify(github.repositories().contents())
                .deleteFile(eq(OWNER), eq(NAME), eq("docs/README.md"), payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo("feature");
    }

    @Test
    void delete_shouldPreferTheGivenBranch_whenTheFileWasReadAtARef() throws IOException {
        final RepositoryFile sut =
                new RepositoryFile(repository, RepositoryContentFixtures.contentFile("base64", "SGVsbG8="), "feature");
        final ArgumentCaptor<GitHubDeleteFilePayload> payload = ArgumentCaptor.forClass(GitHubDeleteFilePayload.class);

        sut.delete("Remove the file", "other");

        verify(github.repositories().contents())
                .deleteFile(eq(OWNER), eq(NAME), eq("docs/README.md"), payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo("other");
    }

    @Test
    void delete_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final RepositoryFile sut = fileOf("Hello".getBytes(StandardCharsets.UTF_8));

        final Throwable thrown = catchThrowable(() -> sut.delete(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }
}
