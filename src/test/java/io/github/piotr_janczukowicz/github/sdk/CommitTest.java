package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommitBuilder;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitTest {

    private final Repository repository = GitHubSdk.of(mock(GitHub.class)).repository("octocat", "hello-world");
    private final GitHubCommit record = RepositoryContentFixtures.commit();
    private final Commit sut = new Commit(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenCommitIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.sha()).isEqualTo("abc123");
            softly.assertThat(sut.message()).isEqualTo("Fix the bug");
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void author_shouldBeEmpty_whenTheRecordHasNoAuthor() {
        final Optional<GitHubSimpleUser> author = sut.author();

        assertThat(author).isEmpty();
    }

    @Test
    void author_shouldReturnTheUser_whenTheRecordHasOne() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        final Commit withAuthor = new Commit(
                repository, GitHubCommitBuilder.builder(record).author(user).build());

        final Optional<GitHubSimpleUser> author = withAuthor.author();

        assertThat(author).containsSame(user);
    }
}
