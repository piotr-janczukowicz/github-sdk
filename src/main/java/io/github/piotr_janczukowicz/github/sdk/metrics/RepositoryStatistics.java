package io.github.piotr_janczukowicz.github.sdk.metrics;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCodeFrequencyStats;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCommitActivity;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubContributorActivity;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubParticipationStats;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubPunchCardStats;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The statistics of a {@link Repository}. GitHub computes them in the background and answers HTTP 202 until they are
 * ready, so the first request after a long time often returns nothing: a method that returns an {@link Optional}
 * returns an empty one, and a method that returns a list returns an empty list. Ask again a little later. The object
 * holds no state, and the results are the client's records.
 */
public final class RepositoryStatistics {

    private final Repository repository;

    public RepositoryStatistics(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Returns the commits, additions and deletions of each contributor per week ({@code GET
     * /repos/{owner}/{repo}/stats/contributors}).
     *
     * @return the contributors, or an empty list while GitHub is still computing them
     * @throws IOException if the transport fails
     */
    public List<GitHubContributorActivity> contributors() throws IOException {
        return github().metrics().statistics().getContributorsStats(repository.owner(), repository.name());
    }

    /**
     * Returns the number of commits per day for the last year, grouped by week ({@code GET
     * /repos/{owner}/{repo}/stats/commit_activity}).
     *
     * @return the weeks, or an empty list while GitHub is still computing them
     * @throws IOException if the transport fails
     */
    public List<GitHubCommitActivity> commitActivity() throws IOException {
        return github().metrics().statistics().getCommitActivityStats(repository.owner(), repository.name());
    }

    /**
     * Returns the additions and deletions per week ({@code GET /repos/{owner}/{repo}/stats/code_frequency}).
     *
     * @return the weeks, or empty while GitHub is still computing them
     * @throws IOException if the transport fails
     */
    public Optional<GitHubCodeFrequencyStats> codeFrequency() throws IOException {
        return github().metrics().statistics().getCodeFrequencyStats(repository.owner(), repository.name());
    }

    /**
     * Returns the weekly commit counts of the owner and of everyone, for the last year ({@code GET
     * /repos/{owner}/{repo}/stats/participation}).
     *
     * @return the participation
     * @throws IOException if the transport fails
     */
    public GitHubParticipationStats participation() throws IOException {
        return github().metrics().statistics().getParticipationStats(repository.owner(), repository.name());
    }

    /**
     * Returns the number of commits for each hour of each weekday ({@code GET /repos/{owner}/{repo}/stats/punch_card}).
     *
     * @return the punch card, or empty while GitHub is still computing it
     * @throws IOException if the transport fails
     */
    public Optional<GitHubPunchCardStats> punchCard() throws IOException {
        return github().metrics().statistics().getPunchCardStats(repository.owner(), repository.name());
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
