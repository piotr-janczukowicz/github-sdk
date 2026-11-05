package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentDirectory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDeleteFilePayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubGetContentFilter;
import io.github.piotr_janczukowicz.github.sdk.content.RepositoryFile;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryFilesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String PATH = "docs/README.md";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubContentFile response = RepositoryContentFixtures.contentFile("base64", "SGVsbG8=");

    @Test
    void file_shouldReturnTheFile_whenGitHubHasIt() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubGetContentFilter> filter = ArgumentCaptor.forClass(GitHubGetContentFilter.class);

        final Optional<RepositoryFile> file = sut.file(PATH);

        verify(github.repositories().contents()).getContent(eq(OWNER), eq(NAME), eq(PATH), filter.capture());
        assertThat(file).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(file.orElseThrow().response()).isSameAs(response);
            softly.assertThat(file.orElseThrow().repository()).isSameAs(sut);
            softly.assertThat(filter.getValue().ref()).isNull();
        });
    }

    @Test
    void file_shouldSendTheRef_whenCalledWithARef() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubGetContentFilter> filter = ArgumentCaptor.forClass(GitHubGetContentFilter.class);

        sut.file(PATH, "feature");

        verify(github.repositories().contents()).getContent(eq(OWNER), eq(NAME), eq(PATH), filter.capture());
        assertThat(filter.getValue().ref()).isEqualTo("feature");
    }

    @Test
    void file_shouldRememberTheRef_whenReadAtARef() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubDeleteFilePayload> payload = ArgumentCaptor.forClass(GitHubDeleteFilePayload.class);

        sut.file(PATH, "feature").orElseThrow().delete("Remove the file");

        verify(github.repositories().contents()).deleteFile(eq(OWNER), eq(NAME), eq(PATH), payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo("feature");
    }

    @Test
    void file_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.empty());

        final Optional<RepositoryFile> file = sut.file(PATH);

        assertThat(file).isEmpty();
    }

    @Test
    void file_shouldReturnEmpty_whenThePathIsADirectory() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq("docs"), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.of(mock(GitHubContentDirectory.class)));

        final Optional<RepositoryFile> file = sut.file("docs");

        assertThat(file).isEmpty();
    }

    @Test
    void file_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.file(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeFile_shouldCreateTheFileWithoutASha_whenItDoesNotExist() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.empty(), Optional.of(response));
        final ArgumentCaptor<GitHubCreateOrUpdateFileContentsPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrUpdateFileContentsPayload.class);

        final RepositoryFile written = sut.writeFile(PATH, "Add the file", "Hello".getBytes(StandardCharsets.UTF_8));

        verify(github.repositories().contents())
                .createOrUpdateFileContents(eq(OWNER), eq(NAME), eq(PATH), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().message()).isEqualTo("Add the file");
            softly.assertThat(payload.getValue().content()).isEqualTo("SGVsbG8=");
            softly.assertThat(payload.getValue().sha()).isNull();
            softly.assertThat(payload.getValue().branch()).isNull();
            softly.assertThat(written.response()).isSameAs(response);
        });
    }

    @Test
    void writeFile_shouldSendTheCurrentShaAndBranch_whenTheFileExists() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubCreateOrUpdateFileContentsPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrUpdateFileContentsPayload.class);
        final ArgumentCaptor<GitHubGetContentFilter> filter = ArgumentCaptor.forClass(GitHubGetContentFilter.class);

        sut.writeFile(PATH, "Change the file", "Hello".getBytes(StandardCharsets.UTF_8), "feature");

        verify(github.repositories().contents())
                .createOrUpdateFileContents(eq(OWNER), eq(NAME), eq(PATH), payload.capture());
        verify(github.repositories().contents(), times(2)).getContent(eq(OWNER), eq(NAME), eq(PATH), filter.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().sha()).isEqualTo("blob123");
            softly.assertThat(payload.getValue().branch()).isEqualTo("feature");
            softly.assertThat(filter.getAllValues())
                    .allSatisfy(each -> assertThat(each.ref()).isEqualTo("feature"));
        });
    }

    @Test
    void writeFile_shouldThrowIllegalStateException_whenTheFileIsGoneAfterTheWrite() throws IOException {
        when(github.repositories()
                        .contents()
                        .getContent(eq(OWNER), eq(NAME), eq(PATH), any(GitHubGetContentFilter.class)))
                .thenReturn(Optional.empty());
        final byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> sut.writeFile(PATH, "Add the file", content));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(PATH);
    }

    @Test
    void writeFile_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.writeFile(PATH, "Add the file", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @Test
    void writeFile_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> sut.writeFile(PATH, " ", content));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }
}
