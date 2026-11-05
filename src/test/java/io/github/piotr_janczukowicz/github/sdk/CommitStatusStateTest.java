package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitStatusStateTest {

    @Test
    void toClient_shouldMapEveryStateToTheClientValue_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(CommitStatusState.ERROR.toClient()).isEqualTo(GitHubCommitStatusState.ERROR);
            softly.assertThat(CommitStatusState.FAILURE.toClient()).isEqualTo(GitHubCommitStatusState.FAILURE);
            softly.assertThat(CommitStatusState.PENDING.toClient()).isEqualTo(GitHubCommitStatusState.PENDING);
            softly.assertThat(CommitStatusState.SUCCESS.toClient()).isEqualTo(GitHubCommitStatusState.SUCCESS);
        });
    }
}
