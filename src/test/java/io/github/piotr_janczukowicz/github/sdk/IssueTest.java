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
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueUpdateState;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueComment;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 42;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubIssue record = IssuesFixtures.issue();
    private final Issue sut = new Issue(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenIssueIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.title()).isEqualTo("Found a bug");
            softly.assertThat(sut.state()).isEqualTo("open");
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void author_shouldBeEmpty_whenTheRecordHasNoUser() {
        final Optional<GitHubSimpleUser> author = sut.author();

        assertThat(author).isEmpty();
    }

    @Test
    void author_shouldReturnTheUser_whenTheRecordHasOne() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        final Issue withAuthor = new Issue(
                repository, GitHubIssueBuilder.builder(record).user(user).build());

        final Optional<GitHubSimpleUser> author = withAuthor.author();

        assertThat(author).containsSame(user);
    }

    @Test
    void close_shouldSendClosedAndReturnTheReloadedIssue_whenUpdateSucceeds() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(record).state("closed").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Issue closed = sut.close();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubIssueUpdateState.CLOSED);
            softly.assertThat(closed.record()).isSameAs(reloaded);
            softly.assertThat(sut.state()).isEqualTo("open");
        });
    }

    @Test
    void reopen_shouldSendOpenAndReturnTheReloadedIssue_whenUpdateSucceeds() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(record).state("open").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Issue reopened = sut.reopen();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubIssueUpdateState.OPEN);
            softly.assertThat(reopened.record()).isSameAs(reloaded);
        });
    }

    @Test
    void close_shouldThrowIllegalStateException_whenTheIssueIsGoneAfterTheUpdate() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::close);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void comment_shouldSendTheBodyAndReturnTheComment_whenCalledWithABody() throws IOException {
        final GitHubIssueComment created = IssuesFixtures.issueComment();
        when(github.issues()
                        .comments()
                        .createComment(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubCreateCommentPayload.class)))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommentPayload.class);

        final IssueComment comment = sut.comment("First comment");

        verify(github.issues().comments()).createComment(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("First comment");
            softly.assertThat(comment.record()).isSameAs(created);
            softly.assertThat(comment.issue()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.comment(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void comments_shouldWrapEachComment_whenTheIssueHasComments() throws IOException {
        final GitHubIssueComment first = IssuesFixtures.issueComment();
        when(github.issues().comments().listComments(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(first));

        final List<IssueComment> comments = sut.comments().toList();

        assertThat(comments).hasSize(1);
        assertSoftly(softly -> {
            softly.assertThat(comments.getFirst().record()).isSameAs(first);
            softly.assertThat(comments.getFirst().issue()).isSameAs(sut);
        });
    }
}
