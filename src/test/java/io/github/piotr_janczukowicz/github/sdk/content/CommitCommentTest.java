package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComment;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitsCommentsApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubUpdateCommitCommentPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitCommentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ExtrasFixtures.COMMENT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCommitComment response = ExtrasFixtures.commitComment();
    private final CommitComment sut = new CommitComment(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenCommentIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.body()).isEqualTo("Nice");
            softly.assertThat(sut.commitId()).isEqualTo(ExtrasFixtures.SHA);
            softly.assertThat(sut.author()).hasValue("octocat");
            softly.assertThat(sut.path()).isEmpty();
            softly.assertThat(sut.line()).isEmpty();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldReportThePlace_whenTheCommentIsOnALine() {
        final CommitComment onLine =
                new CommitComment(repository, ExtrasFixtures.commitCommentWith("src/Main.java", 12));

        assertSoftly(softly -> {
            softly.assertThat(onLine.path()).hasValue("src/Main.java");
            softly.assertThat(onLine.line()).hasValue(12);
        });
    }

    @Test
    void author_shouldBeEmpty_whenGitHubReportsNone() {
        final CommitComment anonymous = new CommitComment(repository, ExtrasFixtures.commitCommentWithoutAuthor());

        assertThat(anonymous.author()).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedComment_whenItExists() throws IOException {
        final GitHubCommitComment reloaded = ExtrasFixtures.commitCommentWith("a.txt", 1);
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(Optional.of(reloaded)).when(api).getCommitComment(OWNER, NAME, ID);

        final CommitComment result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheCommentIsGone() throws IOException {
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(Optional.empty()).when(api).getCommitComment(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void edit_shouldSendTheBodyAndReturnTheComment_whenCalled() throws IOException {
        final GitHubCommitComment edited = ExtrasFixtures.commitCommentWith("a.txt", 1);
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(edited).when(api).updateCommitComment(OWNER, NAME, ID, new GitHubUpdateCommitCommentPayload("Better"));

        final CommitComment result = sut.edit("Better");

        assertThat(result.response()).isSameAs(edited);
    }

    @Test
    void edit_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.edit(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void edit_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.edit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void delete_shouldDeleteTheComment_whenCalled() throws IOException {
        final GitHubCommitsCommentsApi api = github.commits().comments();

        sut.delete();

        verify(api).deleteCommitComment(OWNER, NAME, ID);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new CommitComment(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new CommitComment(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
