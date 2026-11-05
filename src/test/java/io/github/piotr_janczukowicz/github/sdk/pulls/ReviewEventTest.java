package io.github.piotr_janczukowicz.github.sdk.pulls;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewEvent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ReviewEventTest {

    @Test
    void toClient_shouldMapEveryEventToTheClientValue_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(ReviewEvent.APPROVE.toClient()).isEqualTo(GitHubPullRequestReviewEvent.APPROVE);
            softly.assertThat(ReviewEvent.REQUEST_CHANGES.toClient())
                    .isEqualTo(GitHubPullRequestReviewEvent.REQUEST_CHANGES);
            softly.assertThat(ReviewEvent.COMMENT.toClient()).isEqualTo(GitHubPullRequestReviewEvent.COMMENT);
        });
    }
}
