package io.github.piotr_janczukowicz.github.sdk.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusBuilder;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitStatusTest {

    private final Repository repository = GitHubSdk.of(mock(GitHub.class)).repository("octocat", "hello-world");
    private final Commit commit = new Commit(repository, RepositoryContentFixtures.commit());
    private final GitHubCommitStatus response = ChecksFixtures.commitStatus();
    private final CommitStatus sut = new CommitStatus(commit, response);

    @Test
    void accessors_shouldExposeTheResponse_whenStatusIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.commit()).isSameAs(commit);
            softly.assertThat(sut.id()).isEqualTo(6006L);
            softly.assertThat(sut.context()).isEqualTo("ci/build");
            softly.assertThat(sut.state()).isEqualTo("success");
            softly.assertThat(sut.description()).hasValue("Build passed");
            softly.assertThat(sut.targetUrl()).hasValue("https://ci.example.com/build/1");
            softly.assertThat(sut.creator()).containsSame(response.creator());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void description_shouldBeEmpty_whenTheResponseHasNone() {
        final CommitStatus status = bareStatus();

        final Optional<String> description = status.description();

        assertThat(description).isEmpty();
    }

    @Test
    void targetUrl_shouldBeEmpty_whenTheResponseHasNone() {
        final CommitStatus status = bareStatus();

        final Optional<String> targetUrl = status.targetUrl();

        assertThat(targetUrl).isEmpty();
    }

    @Test
    void creator_shouldBeEmpty_whenTheResponseHasNone() {
        final CommitStatus status = bareStatus();

        final Optional<GitHubSimpleUser> creator = status.creator();

        assertThat(creator).isEmpty();
    }

    @Test
    void isSuccess_shouldBeTrue_whenTheStateIsSuccess() {
        final boolean success = sut.isSuccess();

        assertThat(success).isTrue();
    }

    @Test
    void isSuccess_shouldBeFalse_whenTheStateIsFailure() {
        final GitHubCommitStatus failedResponse =
                GitHubCommitStatusBuilder.builder(response).state("failure").build();
        final CommitStatus failed = new CommitStatus(commit, failedResponse);

        final boolean success = failed.isSuccess();

        assertThat(success).isFalse();
    }

    private CommitStatus bareStatus() {
        final GitHubCommitStatus bare = GitHubCommitStatusBuilder.builder(response)
                .description(null)
                .targetUrl(null)
                .creator(null)
                .build();
        return new CommitStatus(commit, bare);
    }
}
