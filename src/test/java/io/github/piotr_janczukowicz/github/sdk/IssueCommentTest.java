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
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateCommentPayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueComment;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueCommentBuilder;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueCommentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 2002L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Issue issue = new Issue(GitHubSdk.of(github).repository(OWNER, NAME), IssuesFixtures.issue());
    private final GitHubIssueComment record = IssuesFixtures.issueComment();
    private final IssueComment sut = new IssueComment(issue, record);

    @Test
    void accessors_shouldExposeTheRecord_whenCommentIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.issue()).isSameAs(issue);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.body()).contains("First comment");
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void body_shouldBeEmpty_whenTheRecordHasNoBody() {
        final IssueComment withoutBody = new IssueComment(
                issue, GitHubIssueCommentBuilder.builder(record).body(null).build());

        final Optional<String> body = withoutBody.body();

        assertThat(body).isEmpty();
    }

    @Test
    void author_shouldBeEmpty_whenTheRecordHasNoUser() {
        final Optional<GitHubSimpleUser> author = sut.author();

        assertThat(author).isEmpty();
    }

    @Test
    void author_shouldReturnTheUser_whenTheRecordHasOne() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        final IssueComment withAuthor = new IssueComment(
                issue, GitHubIssueCommentBuilder.builder(record).user(user).build());

        final Optional<GitHubSimpleUser> author = withAuthor.author();

        assertThat(author).containsSame(user);
    }

    @Test
    void edit_shouldSendTheBodyAndReturnTheUpdatedComment_whenCalledWithABody() throws IOException {
        final GitHubIssueComment updated =
                GitHubIssueCommentBuilder.builder(record).body("Edited").build();
        when(github.issues()
                        .comments()
                        .updateComment(eq(OWNER), eq(NAME), eq(ID), any(GitHubUpdateCommentPayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateCommentPayload.class);

        final IssueComment edited = sut.edit("Edited");

        verify(github.issues().comments()).updateComment(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Edited");
            softly.assertThat(edited.record()).isSameAs(updated);
            softly.assertThat(edited.issue()).isSameAs(issue);
            softly.assertThat(sut.body()).contains("First comment");
        });
    }

    @Test
    void edit_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.edit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void refresh_shouldReturnTheReloadedComment_whenGitHubHasIt() throws IOException {
        final GitHubIssueComment reloaded =
                GitHubIssueCommentBuilder.builder(record).body("Reloaded").build();
        when(github.issues().comments().getComment(OWNER, NAME, ID)).thenReturn(Optional.of(reloaded));

        final IssueComment refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
            softly.assertThat(refreshed.issue()).isSameAs(issue);
            softly.assertThat(sut.body()).contains("First comment");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheCommentIsGone() throws IOException {
        when(github.issues().comments().getComment(OWNER, NAME, ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("2002");
    }

    @Test
    void delete_shouldDeleteTheComment_whenCalled() throws IOException {
        sut.delete();

        verify(github.issues().comments()).deleteComment(OWNER, NAME, ID);
    }
}
