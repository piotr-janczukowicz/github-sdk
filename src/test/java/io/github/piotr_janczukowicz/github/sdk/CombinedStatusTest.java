package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatus;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CombinedStatusTest {

    private final Repository repository = GitHubSdk.of(mock(GitHub.class)).repository("octocat", "hello-world");
    private final Commit commit = new Commit(repository, RepositoryContentFixtures.commit());

    @Test
    void accessors_shouldExposeTheResponse_whenStatusIsLoaded() {
        final GitHubCombinedCommitStatus response = ChecksFixtures.combinedStatus("success");
        final CombinedStatus sut = new CombinedStatus(commit, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.commit()).isSameAs(commit);
            softly.assertThat(sut.state()).isEqualTo("success");
            softly.assertThat(sut.statuses()).isSameAs(response.statuses());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void stateChecks_shouldMatchTheState_whenCalled() {
        final CombinedStatus success = new CombinedStatus(commit, ChecksFixtures.combinedStatus("success"));
        final CombinedStatus failure = new CombinedStatus(commit, ChecksFixtures.combinedStatus("failure"));
        final CombinedStatus pending = new CombinedStatus(commit, ChecksFixtures.combinedStatus("pending"));

        assertSoftly(softly -> {
            softly.assertThat(success.isSuccess()).isTrue();
            softly.assertThat(success.isFailure()).isFalse();
            softly.assertThat(success.isPending()).isFalse();
            softly.assertThat(failure.isFailure()).isTrue();
            softly.assertThat(failure.isSuccess()).isFalse();
            softly.assertThat(pending.isPending()).isTrue();
            softly.assertThat(pending.isSuccess()).isFalse();
        });
    }
}
