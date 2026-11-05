package io.github.piotr_janczukowicz.github.sdk.metrics;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCloneTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubContentTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubGetClonesFilter;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubReferrerTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubTrafficPer;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubViewTraffic;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * The traffic of a {@link Repository} over the last 14 days. GitHub shows it only to people with push access, so every
 * method fails with the client's forbidden exception for anyone else. The object holds no state; every method asks
 * GitHub, and the results are the client's records.
 */
public final class RepositoryTraffic {

    private final Repository repository;

    /**
     * Creates the handle. Applications normally get it from the object it belongs to. Nothing is requested until a
     * method is called.
     *
     * @param repository the repository the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public RepositoryTraffic(final Repository repository) {
        this.repository = requireNonNull(repository, "repository");
    }

    /**
     * Returns the views of the repository per day ({@code GET /repos/{owner}/{repo}/traffic/views}).
     *
     * @return the views
     * @throws IOException if the transport fails
     */
    public GitHubViewTraffic views() throws IOException {
        return github().metrics().traffic().getViews(repository.owner(), repository.name());
    }

    /**
     * Returns the views of the repository ({@code GET /repos/{owner}/{repo}/traffic/views}).
     *
     * @param per the size of one data point, a day or a week
     * @return the views
     * @throws NullPointerException if {@code per} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubViewTraffic views(final GitHubTrafficPer per) throws IOException {
        return github().metrics()
                .traffic()
                .getViews(repository.owner(), repository.name(), new GitHubGetClonesFilter(requireNonNull(per, "per")));
    }

    /**
     * Returns the clones of the repository per day ({@code GET /repos/{owner}/{repo}/traffic/clones}).
     *
     * @return the clones
     * @throws IOException if the transport fails
     */
    public GitHubCloneTraffic clones() throws IOException {
        return github().metrics().traffic().getClones(repository.owner(), repository.name());
    }

    /**
     * Returns the clones of the repository ({@code GET /repos/{owner}/{repo}/traffic/clones}).
     *
     * @param per the size of one data point, a day or a week
     * @return the clones
     * @throws NullPointerException if {@code per} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubCloneTraffic clones(final GitHubTrafficPer per) throws IOException {
        return github().metrics()
                .traffic()
                .getClones(
                        repository.owner(), repository.name(), new GitHubGetClonesFilter(requireNonNull(per, "per")));
    }

    /**
     * Returns the ten most viewed paths ({@code GET /repos/{owner}/{repo}/traffic/popular/paths}).
     *
     * @return the paths, most viewed first
     * @throws IOException if the transport fails
     */
    public List<GitHubContentTraffic> topPaths() throws IOException {
        return github().metrics().traffic().getTopPaths(repository.owner(), repository.name());
    }

    /**
     * Returns the ten sites that send the most visitors ({@code GET /repos/{owner}/{repo}/traffic/popular/referrers}).
     *
     * @return the referrers, most visitors first
     * @throws IOException if the transport fails
     */
    public List<GitHubReferrerTraffic> topReferrers() throws IOException {
        return github().metrics().traffic().getTopReferrers(repository.owner(), repository.name());
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
