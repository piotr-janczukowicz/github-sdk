package io.github.piotr_janczukowicz.github.sdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCodeFrequencyStats;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCommitActivity;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubContributorActivity;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubMetricsStatisticsApi;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubParticipationStats;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubPunchCardStats;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryStatisticsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubMetricsStatisticsApi api = github.metrics().statistics();
    private final RepositoryStatistics sut = repository.statistics();

    @Test
    void contributors_shouldReturnTheContributors_whenGitHubHasComputedThem() throws IOException {
        final GitHubContributorActivity contributor = mock(GitHubContributorActivity.class);
        doReturn(List.of(contributor)).when(api).getContributorsStats(OWNER, NAME);

        final List<GitHubContributorActivity> contributors = sut.contributors();

        assertThat(contributors).containsExactly(contributor);
    }

    @Test
    void contributors_shouldBeEmpty_whenGitHubIsStillComputing() throws IOException {
        doReturn(List.of()).when(api).getContributorsStats(OWNER, NAME);

        final List<GitHubContributorActivity> contributors = sut.contributors();

        assertThat(contributors).isEmpty();
    }

    @Test
    void commitActivity_shouldReturnTheWeeks_whenCalled() throws IOException {
        final GitHubCommitActivity week = mock(GitHubCommitActivity.class);
        doReturn(List.of(week)).when(api).getCommitActivityStats(OWNER, NAME);

        final List<GitHubCommitActivity> weeks = sut.commitActivity();

        assertThat(weeks).containsExactly(week);
    }

    @Test
    void codeFrequency_shouldReturnTheStats_whenGitHubHasComputedThem() throws IOException {
        final GitHubCodeFrequencyStats stats = mock(GitHubCodeFrequencyStats.class);
        doReturn(Optional.of(stats)).when(api).getCodeFrequencyStats(OWNER, NAME);

        final Optional<GitHubCodeFrequencyStats> result = sut.codeFrequency();

        assertThat(result).containsSame(stats);
    }

    @Test
    void codeFrequency_shouldBeEmpty_whenGitHubIsStillComputing() throws IOException {
        doReturn(Optional.empty()).when(api).getCodeFrequencyStats(OWNER, NAME);

        final Optional<GitHubCodeFrequencyStats> result = sut.codeFrequency();

        assertThat(result).isEmpty();
    }

    @Test
    void participation_shouldReturnTheStats_whenCalled() throws IOException {
        final GitHubParticipationStats stats = mock(GitHubParticipationStats.class);
        doReturn(stats).when(api).getParticipationStats(OWNER, NAME);

        final GitHubParticipationStats result = sut.participation();

        assertThat(result).isSameAs(stats);
    }

    @Test
    void punchCard_shouldReturnTheStats_whenGitHubHasComputedThem() throws IOException {
        final GitHubPunchCardStats stats = mock(GitHubPunchCardStats.class);
        doReturn(Optional.of(stats)).when(api).getPunchCardStats(OWNER, NAME);

        final Optional<GitHubPunchCardStats> result = sut.punchCard();

        assertThat(result).containsSame(stats);
    }

    @Test
    void punchCard_shouldBeEmpty_whenGitHubIsStillComputing() throws IOException {
        doReturn(Optional.empty()).when(api).getPunchCardStats(OWNER, NAME);

        final Optional<GitHubPunchCardStats> result = sut.punchCard();

        assertThat(result).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryStatistics(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
