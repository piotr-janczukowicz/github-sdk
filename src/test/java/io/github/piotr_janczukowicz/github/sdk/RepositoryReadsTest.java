package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTagPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTagType;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTag;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCodeownersErrors;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubListActivitiesFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryActivity;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryApi;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.content.RepositoryFile;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.github.sdk.git.Ref;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryReadsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String TAG_SHA = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void readme_shouldWrapTheFile_whenTheRepositoryHasOne() throws IOException {
        final GitHubContentFile file = RepositoryContentFixtures.contentFile("base64", "SGVsbG8=");
        when(github.repositories().contents().getReadme(OWNER, NAME)).thenReturn(Optional.of(file));

        final Optional<RepositoryFile> readme = sut.readme();

        assertThat(readme)
                .hasValueSatisfying(found -> assertThat(found.response()).isSameAs(file));
    }

    @Test
    void readme_shouldReturnEmpty_whenTheRepositoryHasNone() throws IOException {
        when(github.repositories().contents().getReadme(OWNER, NAME)).thenReturn(Optional.empty());

        final Optional<RepositoryFile> readme = sut.readme();

        assertThat(readme).isEmpty();
    }

    @Test
    void readme_shouldWrapTheFile_whenCalledWithADirectory() throws IOException {
        final GitHubContentFile file = RepositoryContentFixtures.contentFile("base64", "SGVsbG8=");
        when(github.repositories().contents().getReadmeInDirectory(OWNER, NAME, "docs"))
                .thenReturn(Optional.of(file));

        final Optional<RepositoryFile> readme = sut.readme("docs");

        assertThat(readme)
                .hasValueSatisfying(found -> assertThat(found.response()).isSameAs(file));
    }

    @Test
    void readme_shouldThrowIllegalArgumentException_whenTheDirectoryHasADotSegment() {
        final Throwable thrown = catchThrowable(() -> sut.readme("../etc"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("directory");
    }

    @Test
    void teams_shouldStreamTheTeams_whenTheyExist() throws IOException {
        final GitHubTeam team = mock(GitHubTeam.class);
        when(github.repositories().repository().listTeams(OWNER, NAME).stream()).thenReturn(Stream.of(team));

        final List<GitHubTeam> teams = sut.teams().toList();

        assertThat(teams).containsExactly(team);
    }

    @Test
    void activities_shouldStreamTheActivities_whenCalledWithoutAFilter() throws IOException {
        final GitHubRepositoryActivity activity = mock(GitHubRepositoryActivity.class);
        when(github.repositories().repository().listActivities(OWNER, NAME).stream())
                .thenReturn(Stream.of(activity));

        final List<GitHubRepositoryActivity> activities = sut.activities().toList();

        assertThat(activities).containsExactly(activity);
    }

    @Test
    void activities_shouldPassTheFilter_whenCalledWithOne() throws IOException {
        final GitHubListActivitiesFilter filter = GitHubListActivitiesFilter.empty();
        final GitHubRepositoryActivity activity = mock(GitHubRepositoryActivity.class);
        when(github
                        .repositories()
                        .repository()
                        .listActivities(eq(OWNER), eq(NAME), eq(filter), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(activity));

        final List<GitHubRepositoryActivity> activities = sut.activities(filter).toList();

        assertThat(activities).containsExactly(activity);
    }

    @Test
    void activities_shouldThrowNullPointerException_whenTheFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.activities(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void codeownersErrors_shouldReturnTheErrors_whenGitHubHasThem() throws IOException {
        final GitHubCodeownersErrors errors = mock(GitHubCodeownersErrors.class);
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(Optional.of(errors)).when(api).codeownersErrors(OWNER, NAME);

        final Optional<GitHubCodeownersErrors> result = sut.codeownersErrors();

        assertThat(result).containsSame(errors);
    }

    @Test
    void createTag_shouldSendACommitTag_whenCalled() throws IOException {
        final GitHubGitTag created = mock(GitHubGitTag.class);
        when(github.gitDatabase().tags().createTag(eq(OWNER), eq(NAME), any())).thenReturn(created);
        final ArgumentCaptor<GitHubCreateTagPayload> payload = ArgumentCaptor.forClass(GitHubCreateTagPayload.class);

        final GitHubGitTag tag = sut.createTag("v1", "First", "abc123");

        verify(github.gitDatabase().tags()).createTag(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(tag).isSameAs(created);
            softly.assertThat(payload.getValue().tag()).isEqualTo("v1");
            softly.assertThat(payload.getValue().message()).isEqualTo("First");
            softly.assertThat(payload.getValue().object()).isEqualTo("abc123");
            softly.assertThat(payload.getValue().type()).isEqualTo(GitHubCreateTagType.COMMIT);
        });
    }

    @Test
    void createTag_shouldThrowIllegalArgumentException_whenTheMessageIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createTag("v1", " ", "abc123"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }

    @Test
    void createAnnotatedTag_shouldCreateTheObjectAndTheReference_whenCalled() throws IOException {
        final GitHubGitTag created = mock(GitHubGitTag.class);
        when(created.sha()).thenReturn(TAG_SHA);
        when(github.gitDatabase().tags().createTag(eq(OWNER), eq(NAME), any())).thenReturn(created);
        when(github.gitDatabase().refs().createRef(eq(OWNER), eq(NAME), any()))
                .thenReturn(GitFixtures.refWith("refs/tags/v1", TAG_SHA));
        final ArgumentCaptor<GitHubCreateRefPayload> payload = ArgumentCaptor.forClass(GitHubCreateRefPayload.class);

        final Ref ref = sut.createAnnotatedTag("v1", "First", "abc123");

        verify(github.gitDatabase().refs()).createRef(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ref()).isEqualTo("refs/tags/v1");
            softly.assertThat(payload.getValue().sha()).isEqualTo(TAG_SHA);
            softly.assertThat(ref.repository()).isSameAs(sut);
        });
    }

    @Test
    void createAnnotatedTag_shouldThrowIllegalArgumentException_whenTheTagHasADotSegment() {
        final Throwable thrown = catchThrowable(() -> sut.createAnnotatedTag("a/../b", "First", "abc123"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tag");
    }

    @Test
    void tagObject_shouldReturnTheTag_whenGitHubHasIt() throws IOException {
        final GitHubGitTag loaded = mock(GitHubGitTag.class);
        when(github.gitDatabase().tags().getTag(OWNER, NAME, TAG_SHA)).thenReturn(Optional.of(loaded));

        final Optional<GitHubGitTag> tag = sut.tagObject(TAG_SHA);

        assertThat(tag).containsSame(loaded);
    }

    @Test
    void tagObject_shouldThrowIllegalArgumentException_whenTheShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.tagObject(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void codeownersErrors_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(Optional.empty()).when(api).codeownersErrors(OWNER, NAME);

        final Optional<GitHubCodeownersErrors> result = sut.codeownersErrors();

        assertThat(result).isEmpty();
    }

    @Test
    void readme_shouldReturnEmpty_whenTheDirectoryHasNone() throws IOException {
        when(github.repositories().contents().getReadmeInDirectory(OWNER, NAME, "docs"))
                .thenReturn(Optional.empty());

        final Optional<RepositoryFile> readme = sut.readme("docs");

        assertThat(readme).isEmpty();
    }

    @Test
    void tagObject_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.gitDatabase().tags().getTag(OWNER, NAME, TAG_SHA)).thenReturn(Optional.empty());

        final Optional<GitHubGitTag> tag = sut.tagObject(TAG_SHA);

        assertThat(tag).isEmpty();
    }

    @Test
    void createTag_shouldThrowIllegalArgumentException_whenTheTagIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createTag(" ", "First", "abc123"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tag");
    }

    @Test
    void createTag_shouldThrowIllegalArgumentException_whenTheCommitShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createTag("v1", "First", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }

    @Test
    void createTag_shouldThrowNullPointerException_whenTheTagIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createTag(null, "First", "abc123"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("tag");
    }

    @Test
    void createAnnotatedTag_shouldThrowIllegalArgumentException_whenTheMessageIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createAnnotatedTag("v1", " ", "abc123"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }

    @Test
    void createAnnotatedTag_shouldThrowIllegalArgumentException_whenTheCommitShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createAnnotatedTag("v1", "First", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sha");
    }
}
