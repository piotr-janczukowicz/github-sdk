package io.github.piotr_janczukowicz.github.sdk.checks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteStatus;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A check suite of a {@link Repository}, the group of check runs that GitHub Apps report for one commit, as GitHub
 * returned it. Creating a suite is left to GitHub Apps, so the object is mostly read-only. It is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class CheckSuite {

    private final Repository repository;
    private final GitHubCheckSuite response;

    public CheckSuite(final Repository repository, final GitHubCheckSuite response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the suite belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the suite id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the sha of the commit the suite ran on.
     *
     * @return the head sha
     */
    public String headSha() {
        return response.headSha();
    }

    /**
     * Returns the branch the suite ran on.
     *
     * @return the branch, or empty when GitHub reports none
     */
    public Optional<String> headBranch() {
        return Optional.ofNullable(response.headBranch());
    }

    /**
     * Returns how far the suite has come, as the client decoded it. The client maps a value it does not know to its own
     * {@code UNRECOGNIZED} constant.
     *
     * @return the status, or empty when GitHub reports none
     */
    public Optional<GitHubCheckSuiteStatus> status() {
        return Optional.ofNullable(response.status());
    }

    /**
     * Returns the outcome of a finished suite, as the client decoded it.
     *
     * @return the conclusion, or empty while the suite has not completed
     */
    public Optional<GitHubCheckSuiteConclusion> conclusion() {
        return Optional.ofNullable(response.conclusion());
    }

    /**
     * Tells whether the suite finished and passed.
     *
     * @return {@code true} when the conclusion is {@code SUCCESS}
     */
    public boolean isSuccessful() {
        return response.conclusion() == GitHubCheckSuiteConclusion.SUCCESS;
    }

    /**
     * Loads the suite again ({@code GET /repos/{owner}/{repo}/check-suites/{check_suite_id}}). This object is not
     * changed.
     *
     * @return the suite as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the suite is gone
     */
    public CheckSuite refresh() throws IOException {
        return repository
                .checkSuite(id())
                .orElseThrow(() -> new IllegalStateException("Check suite " + id() + " is gone"));
    }

    /**
     * Asks the app that owns the suite to run its checks again ({@code POST
     * /repos/{owner}/{repo}/check-suites/{check_suite_id}/rerequest}). The app runs them asynchronously.
     *
     * @throws IOException if the transport fails
     */
    public void rerequest() throws IOException {
        final GitHub github = Internals.github(repository);
        github.checks().suites().rerequestSuite(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCheckSuite response() {
        return response;
    }
}
