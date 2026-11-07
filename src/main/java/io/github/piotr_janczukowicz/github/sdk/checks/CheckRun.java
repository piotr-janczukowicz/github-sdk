package io.github.piotr_janczukowicz.github.sdk.checks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckAnnotation;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunPayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunStatus;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A check run of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response for every field the accessors here do not cover. GitHub lets only GitHub Apps create or change
 * check runs, so {@link #update()}, {@link #complete(GitHubUpdateCheckRunConclusion)} and {@link #rerequest()} fail
 * with HTTP 403 for any other credential.
 */
public final class CheckRun {

    private final Repository repository;
    private final GitHubCheckRun response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CheckRun(final Repository repository, final GitHubCheckRun response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the check run belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the check run id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the name of the check, for example {@code build}.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the sha of the commit the check ran on.
     *
     * @return the head sha
     */
    public String headSha() {
        return response.headSha();
    }

    /**
     * Returns how far the check has come, as the client decoded it. The client maps a value it does not know to its own
     * {@code UNRECOGNIZED} constant.
     *
     * @return the status
     */
    public GitHubCheckRunStatus status() {
        return response.status();
    }

    /**
     * Returns the outcome of a finished check, as the client decoded it. The client maps a value it does not know to
     * its own {@code UNRECOGNIZED} constant.
     *
     * @return the conclusion, or empty while the check has not completed
     */
    public Optional<GitHubCheckRunConclusion> conclusion() {
        return Optional.ofNullable(response.conclusion());
    }

    /**
     * Tells whether the check has finished.
     *
     * @return {@code true} when the status is {@code COMPLETED}
     */
    public boolean isCompleted() {
        return response.status() == GitHubCheckRunStatus.COMPLETED;
    }

    /**
     * Tells whether the check finished and passed.
     *
     * @return {@code true} when the conclusion is {@code SUCCESS}
     */
    public boolean isSuccessful() {
        return response.conclusion() == GitHubCheckRunConclusion.SUCCESS;
    }

    /**
     * Returns the link to the page of the service that ran the check.
     *
     * @return the URL, or empty when there is none
     */
    public Optional<String> detailsUrl() {
        return Optional.ofNullable(response.detailsUrl());
    }

    /**
     * Lists the annotations of the check run ({@code GET /repos/{owner}/{repo}/check-runs/{check_run_id}/annotations}).
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the annotations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubCheckAnnotation> annotations() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.checks().runs().listAnnotations(repository.owner(), repository.name(), id()).stream();
    }

    /**
     * Loads the check run again ({@code GET /repos/{owner}/{repo}/check-runs/{check_run_id}}). This object is not
     * changed.
     *
     * @return the check run as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the check run is gone
     */
    public CheckRun refresh() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.checks()
                .runs()
                .getCheckRun(repository.owner(), repository.name(), id())
                .map(reloaded -> new CheckRun(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Check run " + id() + " is gone"));
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link CheckRunUpdate#send()}.
     *
     * @return a new update for this check run
     */
    public CheckRunUpdate update() {
        return new CheckRunUpdate(this);
    }

    /**
     * Changes the check run with every option the client offers, such as annotations and actions ({@code PATCH
     * /repos/{owner}/{repo}/check-runs/{check_run_id}}). This object is not changed.
     *
     * @param payload the changes, as the client defines them
     * @return the check run as GitHub holds it afterwards
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public CheckRun update(final GitHubUpdateCheckRunPayload payload) throws IOException {
        return apply(requireNonNull(payload, "payload"));
    }

    /**
     * Marks the check run as completed with a conclusion ({@code PATCH
     * /repos/{owner}/{repo}/check-runs/{check_run_id}}). This object is not changed.
     *
     * @param conclusion how the run ended
     * @return the check run as GitHub holds it afterwards
     * @throws NullPointerException if {@code conclusion} is {@code null}
     * @throws IOException if the transport fails
     */
    public CheckRun complete(final GitHubUpdateCheckRunConclusion conclusion) throws IOException {
        return update().status(GitHubUpdateCheckRunStatus.COMPLETED)
                .conclusion(conclusion)
                .send();
    }

    /**
     * Asks GitHub to run the check again ({@code POST /repos/{owner}/{repo}/check-runs/{check_run_id}/rerequest}). The
     * app that created the check run receives a {@code check_run} webhook with the action {@code rerequested}.
     *
     * @throws IOException if the transport fails
     */
    public void rerequest() throws IOException {
        Internals.github(repository).checks().runs().rerequestRun(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubCheckRun response() {
        return response;
    }

    CheckRun apply(final GitHubUpdateCheckRunPayload payload) throws IOException {
        return new CheckRun(
                repository,
                Internals.github(repository)
                        .checks()
                        .runs()
                        .updateCheckRun(repository.owner(), repository.name(), id(), payload));
    }
}
