package io.github.piotr_janczukowicz.github.sdk.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatus;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
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
    void isSuccess_shouldBeTrue_whenTheStateIsSuccess() {
        final CombinedStatus status = combined("success");

        final boolean success = status.isSuccess();

        assertThat(success).isTrue();
    }

    @Test
    void isSuccess_shouldBeFalse_whenTheStateIsFailure() {
        final CombinedStatus status = combined("failure");

        final boolean success = status.isSuccess();

        assertThat(success).isFalse();
    }

    @Test
    void isFailure_shouldBeTrue_whenTheStateIsFailure() {
        final CombinedStatus status = combined("failure");

        final boolean failure = status.isFailure();

        assertThat(failure).isTrue();
    }

    @Test
    void isFailure_shouldBeFalse_whenTheStateIsSuccess() {
        final CombinedStatus status = combined("success");

        final boolean failure = status.isFailure();

        assertThat(failure).isFalse();
    }

    @Test
    void isPending_shouldBeTrue_whenTheStateIsPending() {
        final CombinedStatus status = combined("pending");

        final boolean pending = status.isPending();

        assertThat(pending).isTrue();
    }

    @Test
    void isPending_shouldBeFalse_whenTheStateIsSuccess() {
        final CombinedStatus status = combined("success");

        final boolean pending = status.isPending();

        assertThat(pending).isFalse();
    }

    private CombinedStatus combined(final String state) {
        return new CombinedStatus(commit, ChecksFixtures.combinedStatus(state));
    }
}
