package io.github.piotr_janczukowicz.github.sdk.content;

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
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComment;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitsCommentsApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCreateCommitCommentPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckSuite;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CommitCommentsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String SHA = ChecksFixtures.SHA;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Commit sut = new Commit(repository, RepositoryContentFixtures.commit());

    @Test
    void comments_shouldStreamTheComments_whenTheyExist() throws IOException {
        final GitHubCommitComment listed = ExtrasFixtures.commitComment();
        when(github
                        .commits()
                        .comments()
                        .listCommentsForCommit(eq(OWNER), eq(NAME), eq(SHA), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<CommitComment> comments = sut.comments().toList();

        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.response()).isSameAs(listed);
            assertThat(comment.repository()).isSameAs(repository);
        });
    }

    @Test
    void comment_shouldCommentOnTheWholeCommit_whenOnlyTheBodyIsGiven() throws IOException {
        final GitHubCommitComment created = ExtrasFixtures.commitComment();
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(created)
                .when(api)
                .createCommitComment(eq(OWNER), eq(NAME), eq(SHA), any(GitHubCreateCommitCommentPayload.class));
        final ArgumentCaptor<GitHubCreateCommitCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommitCommentPayload.class);

        final CommitComment comment = sut.comment("Nice");

        verify(api).createCommitComment(eq(OWNER), eq(NAME), eq(SHA), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Nice");
            softly.assertThat(payload.getValue().path()).isNull();
            softly.assertThat(payload.getValue().line()).isNull();
            softly.assertThat(comment.response()).isSameAs(created);
        });
    }

    @Test
    void comment_shouldCommentOnALine_whenPathAndLineAreGiven() throws IOException {
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(ExtrasFixtures.commitComment())
                .when(api)
                .createCommitComment(eq(OWNER), eq(NAME), eq(SHA), any(GitHubCreateCommitCommentPayload.class));
        final ArgumentCaptor<GitHubCreateCommitCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommitCommentPayload.class);

        sut.comment("Look here", "src/Main.java", 12);

        verify(api).createCommitComment(eq(OWNER), eq(NAME), eq(SHA), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().path()).isEqualTo("src/Main.java");
            softly.assertThat(payload.getValue().line()).isEqualTo(12);
        });
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment("Look here", " ", 1));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @Test
    void checkSuites_shouldStreamTheSuites_whenTheyExist() throws IOException {
        final GitHubCheckSuite listed = ExtrasFixtures.checkSuite();
        when(github.checks().suites().listSuitesForRef(OWNER, NAME, SHA).stream())
                .thenReturn(Stream.of(listed));

        final List<CheckSuite> suites = sut.checkSuites().toList();

        assertThat(suites).singleElement().satisfies(suite -> {
            assertThat(suite.response()).isSameAs(listed);
            assertThat(suite.repository()).isSameAs(repository);
        });
    }
}
