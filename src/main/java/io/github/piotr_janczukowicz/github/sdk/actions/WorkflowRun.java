package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobFilterMode;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListJobsForWorkflowRunFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubReRunWorkflowFailedJobsPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubReRunWorkflowPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRun;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * A workflow run of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response for every field the accessors here do not cover. The client types the status and the conclusion
 * of a run as text, so they are kept as GitHub wrote them, for example {@code completed} and {@code success}.
 */
public final class WorkflowRun {

    private static final String COMPLETED = "completed";
    private static final String SUCCESS = "success";

    private final Repository repository;
    private final GitHubWorkflowRun response;

    public WorkflowRun(final Repository repository, final GitHubWorkflowRun response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the run belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the run id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the id of the workflow the run belongs to.
     *
     * @return the workflow id
     */
    public long workflowId() {
        return response.workflowId();
    }

    /**
     * Returns the name of the run.
     *
     * @return the name, or empty when GitHub reports none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the branch the run was started for.
     *
     * @return the branch, or empty when GitHub reports none
     */
    public Optional<String> headBranch() {
        return Optional.ofNullable(response.headBranch());
    }

    /**
     * Returns the sha of the commit the run was started for.
     *
     * @return the head sha
     */
    public String headSha() {
        return response.headSha();
    }

    /**
     * Returns the event that started the run, for example {@code push} or {@code workflow_dispatch}.
     *
     * @return the event
     */
    public String event() {
        return response.event();
    }

    /**
     * Returns how far the run has come, as GitHub wrote it, for example {@code queued} or {@code completed}.
     *
     * @return the status, or empty when GitHub reports none
     */
    public Optional<String> status() {
        return Optional.ofNullable(response.status());
    }

    /**
     * Returns the outcome of a finished run, as GitHub wrote it, for example {@code success} or {@code failure}.
     *
     * @return the conclusion, or empty while the run has not completed
     */
    public Optional<String> conclusion() {
        return Optional.ofNullable(response.conclusion());
    }

    /**
     * Tells whether the run has finished.
     *
     * @return {@code true} when the status is {@code completed}
     */
    public boolean isCompleted() {
        return COMPLETED.equals(response.status());
    }

    /**
     * Tells whether the run finished and passed.
     *
     * @return {@code true} when the conclusion is {@code success}
     */
    public boolean isSuccessful() {
        return SUCCESS.equals(response.conclusion());
    }

    /**
     * Returns the number of the run within its workflow.
     *
     * @return the run number
     */
    public int runNumber() {
        return response.runNumber();
    }

    /**
     * Returns the attempt of the run, which counts up with every re-run.
     *
     * @return the attempt, or empty when GitHub reports none
     */
    public OptionalInt runAttempt() {
        final Integer attempt = response.runAttempt();
        return attempt == null ? OptionalInt.empty() : OptionalInt.of(attempt);
    }

    /**
     * Returns the link to the run on GitHub.
     *
     * @return the URL
     */
    public String htmlUrl() {
        return response.htmlUrl();
    }

    /**
     * Loads the run again ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}}). This object is not changed.
     *
     * @return the run as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun refresh() throws IOException {
        final GitHub github = github();
        return github.actions()
                .workflowRuns()
                .getWorkflowRun(repository.owner(), repository.name(), id())
                .map(reloaded -> new WorkflowRun(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Workflow run " + id() + " is gone"));
    }

    /**
     * Loads one earlier attempt of the run ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/attempts/{n}}).
     *
     * @param attempt the attempt number
     * @return the attempt, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<WorkflowRun> attempt(final long attempt) throws IOException {
        final GitHub github = github();
        return github.actions()
                .workflowRuns()
                .getWorkflowRunAttempt(repository.owner(), repository.name(), id(), attempt)
                .map(found -> new WorkflowRun(repository, found));
    }

    /**
     * Cancels the run ({@code POST /repos/{owner}/{repo}/actions/runs/{run_id}/cancel}). GitHub stops a run
     * asynchronously, so the returned run may still be in progress. This object is not changed.
     *
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun cancel() throws IOException {
        final GitHub github = github();
        github.actions().workflowRuns().cancelWorkflowRun(repository.owner(), repository.name(), id());
        return refresh();
    }

    /**
     * Cancels the run without waiting for its conditions ({@code POST
     * /repos/{owner}/{repo}/actions/runs/{run_id}/force-cancel}). Use it when {@link #cancel()} does not stop the run.
     * This object is not changed.
     *
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun forceCancel() throws IOException {
        final GitHub github = github();
        github.actions().workflowRuns().forceCancelWorkflowRun(repository.owner(), repository.name(), id());
        return refresh();
    }

    /**
     * Runs all jobs again. See {@link #rerun(boolean)}.
     *
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun rerun() throws IOException {
        return rerun(false);
    }

    /**
     * Runs all jobs again ({@code POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun}). This object is not changed.
     *
     * @param isDebugLoggingEnabled whether the new attempt writes debug logs
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun rerun(final boolean isDebugLoggingEnabled) throws IOException {
        final GitHub github = github();
        github.actions()
                .workflowRuns()
                .reRunWorkflow(
                        repository.owner(),
                        repository.name(),
                        id(),
                        new GitHubReRunWorkflowPayload(isDebugLoggingEnabled));
        return refresh();
    }

    /**
     * Runs the failed jobs and the jobs that depend on them again. See {@link #rerunFailedJobs(boolean)}.
     *
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun rerunFailedJobs() throws IOException {
        return rerunFailedJobs(false);
    }

    /**
     * Runs the failed jobs and the jobs that depend on them again ({@code POST
     * /repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed-jobs}). This object is not changed.
     *
     * @param isDebugLoggingEnabled whether the new attempt writes debug logs
     * @return the run as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the run is gone
     */
    public WorkflowRun rerunFailedJobs(final boolean isDebugLoggingEnabled) throws IOException {
        final GitHub github = github();
        github.actions()
                .workflowRuns()
                .reRunWorkflowFailedJobs(
                        repository.owner(),
                        repository.name(),
                        id(),
                        new GitHubReRunWorkflowFailedJobsPayload(isDebugLoggingEnabled));
        return refresh();
    }

    /**
     * Deletes the run ({@code DELETE /repos/{owner}/{repo}/actions/runs/{run_id}}). GitHub deletes only a finished run.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = github();
        github.actions().workflowRuns().deleteWorkflowRun(repository.owner(), repository.name(), id());
    }

    /**
     * Lists the jobs of the latest attempt ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the jobs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowJob> jobs() throws IOException {
        final GitHub github = github();
        return github
                .actions()
                .workflowJobs()
                .listJobsForWorkflowRun(
                        repository.owner(),
                        repository.name(),
                        id(),
                        new GitHubListJobsForWorkflowRunFilter(GitHubJobFilterMode.LATEST),
                        GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new WorkflowJob(repository, listed));
    }

    /**
     * Lists the jobs of one attempt ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/attempts/{n}/jobs}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param attempt the attempt number
     * @return the jobs of that attempt
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowJob> jobs(final long attempt) throws IOException {
        final GitHub github = github();
        return github
                .actions()
                .workflowJobs()
                .listJobsForWorkflowRunAttempt(
                        repository.owner(), repository.name(), id(), attempt, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new WorkflowJob(repository, listed));
    }

    /**
     * Lists the artifacts of the run ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/artifacts}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the artifacts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Artifact> artifacts() throws IOException {
        final GitHub github = github();
        return github
                .actions()
                .artifacts()
                .listWorkflowRunArtifacts(repository.owner(), repository.name(), id(), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Artifact(repository, listed));
    }

    /**
     * Asks for the link to the logs of the latest attempt as a zip archive ({@code GET
     * /repos/{owner}/{repo}/actions/runs/{run_id}/logs}). The link is short-lived, and this method downloads nothing.
     *
     * @return the link GitHub redirects to
     * @throws IOException if the transport fails
     */
    public URI logs() throws IOException {
        final GitHub github = github();
        return github.actions().workflowRuns().downloadWorkflowRunLogs(repository.owner(), repository.name(), id());
    }

    /**
     * Deletes the logs of the run ({@code DELETE /repos/{owner}/{repo}/actions/runs/{run_id}/logs}).
     *
     * @throws IOException if the transport fails
     */
    public void deleteLogs() throws IOException {
        final GitHub github = github();
        github.actions().workflowRuns().deleteWorkflowRunLogs(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubWorkflowRun response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
