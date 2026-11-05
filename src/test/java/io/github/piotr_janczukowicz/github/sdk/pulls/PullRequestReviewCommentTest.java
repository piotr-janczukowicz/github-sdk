package io.github.piotr_janczukowicz.github.sdk.pulls;

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
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreateReviewCommentReplyPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewCommentBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullsCommentsApi;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdateReviewCommentPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PullRequestsFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReviewsFixtures;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestReviewCommentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;
    private static final long ID = ReviewsFixtures.COMMENT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final PullRequest pullRequest = new PullRequest(repository, PullRequestsFixtures.pullRequest());
    private final GitHubPullRequestReviewComment response = ReviewsFixtures.comment();
    private final PullRequestReviewComment sut = new PullRequestReviewComment(pullRequest, response);

    @Test
    void accessors_shouldExposeTheResponse_whenCommentIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.pullRequest()).isSameAs(pullRequest);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.body()).isEqualTo("Rename this");
            softly.assertThat(sut.path()).isEqualTo("src/Main.java");
            softly.assertThat(sut.line()).hasValue(12);
            softly.assertThat(sut.author()).containsSame(response.user().value());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void author_shouldBeEmpty_whenTheUserIsMissing() {
        final GitHubPullRequestReviewComment withoutUser = GitHubPullRequestReviewCommentBuilder.builder(response)
                .user(null)
                .build();
        final PullRequestReviewComment comment = new PullRequestReviewComment(pullRequest, withoutUser);

        assertThat(comment.author()).isEmpty();
    }

    @Test
    void author_shouldBeEmpty_whenTheUserIsExplicitlyNull() {
        final GitHubPullRequestReviewComment withNullUser = GitHubPullRequestReviewCommentBuilder.builder(response)
                .user(OptionalNull.ofNull())
                .build();
        final PullRequestReviewComment comment = new PullRequestReviewComment(pullRequest, withNullUser);

        assertThat(comment.author()).isEmpty();
    }

    @Test
    void line_shouldBeEmpty_whenTheCommentIsOnTheWholeFile() {
        final GitHubPullRequestReviewComment withoutLine = GitHubPullRequestReviewCommentBuilder.builder(response)
                .line(null)
                .build();
        final PullRequestReviewComment comment = new PullRequestReviewComment(pullRequest, withoutLine);

        assertThat(comment.line()).isEmpty();
    }

    @Test
    void edit_shouldSendTheBodyAndReturnTheUpdatedComment_whenCalled() throws IOException {
        final GitHubPullRequestReviewComment updated = GitHubPullRequestReviewCommentBuilder.builder(response)
                .body("Fixed")
                .build();
        when(github.pullRequests().comments().update(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateReviewCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReviewCommentPayload.class);

        final PullRequestReviewComment result = sut.edit("Fixed");

        verify(github.pullRequests().comments()).update(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Fixed");
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
        });
    }

    @Test
    void edit_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.edit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void reply_shouldSendTheBodyAndReturnTheReply_whenCalled() throws IOException {
        final GitHubPullRequestReviewComment reply = GitHubPullRequestReviewCommentBuilder.builder(response)
                .id(92L)
                .body("Done")
                .build();
        when(github.pullRequests().comments().createReply(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), any()))
                .thenReturn(reply);
        final ArgumentCaptor<GitHubCreateReviewCommentReplyPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateReviewCommentReplyPayload.class);

        final PullRequestReviewComment result = sut.reply("Done");

        verify(github.pullRequests().comments())
                .createReply(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Done");
            softly.assertThat(result.response()).isSameAs(reply);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
        });
    }

    @Test
    void reply_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.reply(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void delete_shouldDeleteTheComment_whenCalled() throws IOException {
        sut.delete();

        verify(github.pullRequests().comments()).delete(OWNER, NAME, ID);
    }

    @Test
    void refresh_shouldReturnTheReloadedComment_whenItExists() throws IOException {
        final GitHubPullRequestReviewComment reloaded = GitHubPullRequestReviewCommentBuilder.builder(response)
                .body("Edited elsewhere")
                .build();
        final GitHubPullsCommentsApi comments = github.pullRequests().comments();
        doReturn(Optional.of(reloaded)).when(comments).get(OWNER, NAME, ID);

        final PullRequestReviewComment result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheCommentIsGone() throws IOException {
        final GitHubPullsCommentsApi comments = github.pullRequests().comments();
        doReturn(Optional.empty()).when(comments).get(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }
}
