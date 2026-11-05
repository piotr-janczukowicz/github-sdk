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
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubDismissReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReview;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewEvent;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullsCommentsApi;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullsReviewsApi;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubSubmitReviewPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdateReviewPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PullRequestsFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReviewsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestReviewTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 7;
    private static final long ID = ReviewsFixtures.REVIEW_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final PullRequest pullRequest = new PullRequest(repository, PullRequestsFixtures.pullRequest());
    private final GitHubPullRequestReview response = ReviewsFixtures.review();
    private final PullRequestReview sut = new PullRequestReview(pullRequest, response);

    @Test
    void accessors_shouldExposeTheResponse_whenReviewIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.pullRequest()).isSameAs(pullRequest);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.state()).isEqualTo("APPROVED");
            softly.assertThat(sut.body()).isEqualTo("Looks good");
            softly.assertThat(sut.author()).containsSame(response.user());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void author_shouldBeEmpty_whenTheUserIsMissing() {
        final GitHubPullRequestReview withoutUser =
                GitHubPullRequestReviewBuilder.builder(response).user(null).build();
        final PullRequestReview review = new PullRequestReview(pullRequest, withoutUser);

        assertThat(review.author()).isEmpty();
    }

    @Test
    void isApproved_shouldBeTrue_whenTheStateIsApproved() {
        final boolean result = sut.isApproved();

        assertThat(result).isTrue();
    }

    @Test
    void isApproved_shouldBeFalse_whenTheStateIsDismissed() {
        final PullRequestReview dismissed = review("DISMISSED");

        final boolean result = dismissed.isApproved();

        assertThat(result).isFalse();
    }

    @Test
    void isDismissed_shouldBeTrue_whenTheStateIsDismissed() {
        final PullRequestReview dismissed = review("DISMISSED");

        final boolean result = dismissed.isDismissed();

        assertThat(result).isTrue();
    }

    @Test
    void isDismissed_shouldBeFalse_whenTheStateIsApproved() {
        final boolean result = sut.isDismissed();

        assertThat(result).isFalse();
    }

    @Test
    void isPending_shouldBeTrue_whenTheStateIsPending() {
        final PullRequestReview pending = review("PENDING");

        final boolean result = pending.isPending();

        assertThat(result).isTrue();
    }

    @Test
    void isPending_shouldBeFalse_whenTheStateIsApproved() {
        final boolean result = sut.isPending();

        assertThat(result).isFalse();
    }

    @Test
    void edit_shouldSendTheBodyAndReturnTheUpdatedReview_whenCalled() throws IOException {
        final GitHubPullRequestReview updated = review("PENDING").response();
        when(github.pullRequests().reviews().update(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), any()))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateReviewPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateReviewPayload.class);

        final PullRequestReview result = sut.edit("Needs work");

        verify(github.pullRequests().reviews()).update(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Needs work");
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
    void submit_shouldSendTheEventAndBody_whenBothAreGiven() throws IOException {
        final GitHubPullRequestReview submitted = review("CHANGES_REQUESTED").response();
        when(github.pullRequests().reviews().submit(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), any()))
                .thenReturn(submitted);
        final ArgumentCaptor<GitHubSubmitReviewPayload> payload =
                ArgumentCaptor.forClass(GitHubSubmitReviewPayload.class);

        final PullRequestReview result = sut.submit(GitHubPullRequestReviewEvent.REQUEST_CHANGES, "Please fix");

        verify(github.pullRequests().reviews()).submit(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().event()).isEqualTo(GitHubPullRequestReviewEvent.REQUEST_CHANGES);
            softly.assertThat(payload.getValue().body()).isEqualTo("Please fix");
            softly.assertThat(result.response()).isSameAs(submitted);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
        });
    }

    @Test
    void submit_shouldSendNoBody_whenOnlyTheEventIsGiven() throws IOException {
        when(github.pullRequests().reviews().submit(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubSubmitReviewPayload> payload =
                ArgumentCaptor.forClass(GitHubSubmitReviewPayload.class);

        sut.submit(GitHubPullRequestReviewEvent.APPROVE);

        verify(github.pullRequests().reviews()).submit(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().event()).isEqualTo(GitHubPullRequestReviewEvent.APPROVE);
            softly.assertThat(payload.getValue().body()).isNull();
        });
    }

    @Test
    void submit_shouldThrowNullPointerException_whenEventIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.submit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("event");
    }

    @Test
    void dismiss_shouldSendTheMessageAndReturnTheDismissedReview_whenCalled() throws IOException {
        final GitHubPullRequestReview dismissed = review("DISMISSED").response();
        when(github.pullRequests().reviews().dismiss(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), any()))
                .thenReturn(dismissed);
        final ArgumentCaptor<GitHubDismissReviewPayload> payload =
                ArgumentCaptor.forClass(GitHubDismissReviewPayload.class);

        final PullRequestReview result = sut.dismiss("Outdated");

        verify(github.pullRequests().reviews()).dismiss(eq(OWNER), eq(NAME), eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().message()).isEqualTo("Outdated");
            softly.assertThat(result.response()).isSameAs(dismissed);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
        });
    }

    @Test
    void dismiss_shouldThrowNullPointerException_whenMessageIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dismiss(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("message");
    }

    @Test
    void deletePending_shouldDeleteTheReview_whenCalled() throws IOException {
        sut.deletePending();

        verify(github.pullRequests().reviews()).deletePending(OWNER, NAME, NUMBER, ID);
    }

    @Test
    void comments_shouldLoadEachListedComment_whenTheReviewHasComments() throws IOException {
        final GitHubReviewComment listed = mock(GitHubReviewComment.class);
        when(listed.id()).thenReturn(ReviewsFixtures.COMMENT_ID);
        final GitHubPullRequestReviewComment loaded = ReviewsFixtures.comment();
        when(github.pullRequests().reviews().listComments(OWNER, NAME, NUMBER, ID).stream())
                .thenReturn(Stream.of(listed));
        final GitHubPullsCommentsApi comments = github.pullRequests().comments();
        doReturn(Optional.of(loaded)).when(comments).get(OWNER, NAME, ReviewsFixtures.COMMENT_ID);

        final List<PullRequestReviewComment> result = sut.comments().toList();

        assertThat(result)
                .singleElement()
                .satisfies(comment -> assertSoftly(softly -> {
                    softly.assertThat(comment.response()).isSameAs(loaded);
                    softly.assertThat(comment.pullRequest()).isSameAs(pullRequest);
                }));
    }

    @Test
    void comments_shouldSkipTheItem_whenAListedCommentIsGone() throws IOException {
        final GitHubReviewComment listed = mock(GitHubReviewComment.class);
        when(listed.id()).thenReturn(ReviewsFixtures.COMMENT_ID);
        when(github.pullRequests().reviews().listComments(OWNER, NAME, NUMBER, ID).stream())
                .thenReturn(Stream.of(listed));
        final GitHubPullsCommentsApi comments = github.pullRequests().comments();
        doReturn(Optional.empty()).when(comments).get(OWNER, NAME, ReviewsFixtures.COMMENT_ID);

        final long count = sut.comments().count();

        assertThat(count).isZero();
    }

    @Test
    void refresh_shouldReturnTheReloadedReview_whenItExists() throws IOException {
        final GitHubPullRequestReview reloaded = review("DISMISSED").response();
        final GitHubPullsReviewsApi reviews = github.pullRequests().reviews();
        doReturn(Optional.of(reloaded)).when(reviews).get(OWNER, NAME, NUMBER, ID);

        final PullRequestReview result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.pullRequest()).isSameAs(pullRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheReviewIsGone() throws IOException {
        final GitHubPullsReviewsApi reviews = github.pullRequests().reviews();
        doReturn(Optional.empty()).when(reviews).get(OWNER, NAME, NUMBER, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    private PullRequestReview review(final String state) {
        return new PullRequestReview(pullRequest, ReviewsFixtures.reviewInState(state));
    }

    @Test
    void edit_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.edit(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("body");
    }

    @Test
    void dismiss_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.dismiss(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }
}
