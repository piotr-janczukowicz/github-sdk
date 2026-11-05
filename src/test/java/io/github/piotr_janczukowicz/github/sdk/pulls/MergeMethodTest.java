package io.github.piotr_janczukowicz.github.sdk.pulls;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeMethod;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class MergeMethodTest {

    @Test
    void toClient_shouldMapEveryMethodToTheClientValue_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(MergeMethod.MERGE.toClient()).isEqualTo(GitHubPullRequestMergeMethod.MERGE);
            softly.assertThat(MergeMethod.SQUASH.toClient()).isEqualTo(GitHubPullRequestMergeMethod.SQUASH);
            softly.assertThat(MergeMethod.REBASE.toClient()).isEqualTo(GitHubPullRequestMergeMethod.REBASE);
        });
    }
}
