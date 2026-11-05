package io.github.piotr_janczukowicz.github.sdk.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparison;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparisonStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ComparisonTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String BASE_HEAD = "main...feature";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCommitComparison response = GitFixtures.comparison();
    private final Comparison sut = new Comparison(repository, BASE_HEAD, response);

    @Test
    void accessors_shouldExposeTheResponse_whenComparisonIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.status()).isEqualTo(GitHubCommitComparisonStatus.AHEAD);
            softly.assertThat(sut.aheadBy()).isEqualTo(2);
            softly.assertThat(sut.behindBy()).isZero();
            softly.assertThat(sut.totalCommits()).isEqualTo(2);
            softly.assertThat(sut.files()).hasSize(1);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void commits_shouldWrapTheEmbeddedCommits_whenCalled() {
        final GitHubCommit embedded = response.commits().getFirst();

        assertThat(sut.commits()).singleElement().satisfies(commit -> {
            assertThat(commit.response()).isSameAs(embedded);
            assertThat(commit.repository()).isSameAs(repository);
        });
    }

    @Test
    void diff_shouldReturnTheDiffText_whenGitHubHasIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.of("diff --git a/x b/x")).when(api).compareCommitsDiff(OWNER, NAME, BASE_HEAD);

        final Optional<String> diff = sut.diff();

        assertThat(diff).hasValue("diff --git a/x b/x");
    }

    @Test
    void patch_shouldReturnThePatchText_whenGitHubHasIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.of("From abc Mon Sep 17")).when(api).compareCommitsPatch(OWNER, NAME, BASE_HEAD);

        final Optional<String> patch = sut.patch();

        assertThat(patch).hasValue("From abc Mon Sep 17");
    }
}
