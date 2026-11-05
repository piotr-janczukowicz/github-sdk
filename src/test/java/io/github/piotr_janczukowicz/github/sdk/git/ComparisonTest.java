package io.github.piotr_janczukowicz.github.sdk.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparison;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparisonStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.fixtures.GitFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
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
    void repository_shouldReturnTheComparedRepository_whenComparisonIsLoaded() {
        final Repository result = sut.repository();

        assertThat(result).isSameAs(repository);
    }

    @Test
    void status_shouldReturnTheStatus_whenComparisonIsLoaded() {
        final GitHubCommitComparisonStatus status = sut.status();

        assertThat(status).isEqualTo(GitHubCommitComparisonStatus.AHEAD);
    }

    @Test
    void aheadBy_shouldReturnTheCount_whenComparisonIsLoaded() {
        final int aheadBy = sut.aheadBy();

        assertThat(aheadBy).isEqualTo(2);
    }

    @Test
    void behindBy_shouldReturnTheCount_whenComparisonIsLoaded() {
        final int behindBy = sut.behindBy();

        assertThat(behindBy).isZero();
    }

    @Test
    void totalCommits_shouldReturnTheCount_whenComparisonIsLoaded() {
        final int totalCommits = sut.totalCommits();

        assertThat(totalCommits).isEqualTo(2);
    }

    @Test
    void files_shouldReturnTheFiles_whenComparisonIsLoaded() {
        final List<GitHubDiffEntry> files = sut.files();

        assertThat(files).hasSize(1);
    }

    @Test
    void response_shouldReturnTheResponse_whenComparisonIsLoaded() {
        final GitHubCommitComparison result = sut.response();

        assertThat(result).isSameAs(response);
    }

    @Test
    void files_shouldBeEmpty_whenTheResponseHasNoFiles() {
        final GitHubCommitComparison bare = mock(GitHubCommitComparison.class);
        when(bare.files()).thenReturn(null);
        final Comparison comparison = new Comparison(repository, BASE_HEAD, bare);

        final List<GitHubDiffEntry> files = comparison.files();

        assertThat(files).isEmpty();
    }

    @Test
    void commits_shouldWrapTheEmbeddedCommits_whenCalled() {
        final GitHubCommit embedded = response.commits().getFirst();

        final List<Commit> commits = sut.commits();

        assertSoftly(softly -> {
            softly.assertThat(commits).hasSize(1);
            softly.assertThat(commits.getFirst().response()).isSameAs(embedded);
            softly.assertThat(commits.getFirst().repository()).isSameAs(repository);
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
    void diff_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.empty()).when(api).compareCommitsDiff(OWNER, NAME, BASE_HEAD);

        final Optional<String> diff = sut.diff();

        assertThat(diff).isEmpty();
    }

    @Test
    void patch_shouldReturnThePatchText_whenGitHubHasIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.of("From abc Mon Sep 17")).when(api).compareCommitsPatch(OWNER, NAME, BASE_HEAD);

        final Optional<String> patch = sut.patch();

        assertThat(patch).hasValue("From abc Mon Sep 17");
    }

    @Test
    void patch_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubCommitApi api = github.commits().commit();
        doReturn(Optional.empty()).when(api).compareCommitsPatch(OWNER, NAME, BASE_HEAD);

        final Optional<String> patch = sut.patch();

        assertThat(patch).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new Comparison(null, BASE_HEAD, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenBaseHeadIsBlank() {
        final Throwable thrown = catchThrowable(() -> new Comparison(repository, " ", response));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("baseHead");
    }
}
