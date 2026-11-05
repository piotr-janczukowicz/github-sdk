package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJob;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobConclusion;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobStatus;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobStep;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * A job of a {@link WorkflowRun}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class WorkflowJob {

    private final Repository repository;
    private final GitHubJob response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public WorkflowJob(final Repository repository, final GitHubJob response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the job belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the job id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the id of the run the job belongs to.
     *
     * @return the run id
     */
    public long runId() {
        return response.runId();
    }

    /**
     * Returns the name of the job.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns how far the job has come, as the client decoded it. The client maps a value it does not know to its own
     * {@code UNRECOGNIZED} constant.
     *
     * @return the status
     */
    public GitHubJobStatus status() {
        return response.status();
    }

    /**
     * Returns the outcome of a finished job, as the client decoded it. The client maps a value it does not know to its
     * own {@code UNRECOGNIZED} constant.
     *
     * @return the conclusion, or empty while the job has not completed
     */
    public Optional<GitHubJobConclusion> conclusion() {
        return Optional.ofNullable(response.conclusion());
    }

    /**
     * Tells whether the job has finished.
     *
     * @return {@code true} when the status is {@code COMPLETED}
     */
    public boolean isCompleted() {
        return response.status() == GitHubJobStatus.COMPLETED;
    }

    /**
     * Tells whether the job finished and passed.
     *
     * @return {@code true} when the conclusion is {@code SUCCESS}
     */
    public boolean isSuccessful() {
        return response.conclusion() == GitHubJobConclusion.SUCCESS;
    }

    /**
     * Returns the steps of the job.
     *
     * @return the steps, empty when GitHub reports none
     */
    public List<GitHubJobStep> steps() {
        final List<GitHubJobStep> steps = response.steps();
        return steps == null ? List.of() : steps;
    }

    /**
     * Loads the job again ({@code GET /repos/{owner}/{repo}/actions/jobs/{job_id}}). This object is not changed.
     *
     * @return the job as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the job is gone
     */
    public WorkflowJob refresh() throws IOException {
        return github().actions()
                .workflowJobs()
                .getJobForWorkflowRun(repository.owner(), repository.name(), id())
                .map(reloaded -> new WorkflowJob(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Workflow job " + id() + " is gone"));
    }

    /**
     * Runs the job again ({@code POST /repos/{owner}/{repo}/actions/jobs/{job_id}/rerun}). This object is not changed.
     *
     * @return the job as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the job is gone
     */
    public WorkflowJob rerun() throws IOException {
        github().actions().workflowRuns().reRunJobForWorkflowRun(repository.owner(), repository.name(), id());
        return refresh();
    }

    /**
     * Asks for the link to the logs of the job as plain text ({@code GET
     * /repos/{owner}/{repo}/actions/jobs/{job_id}/logs}). The link is short-lived, and this method downloads nothing.
     *
     * @return the link GitHub redirects to
     * @throws IOException if the transport fails
     */
    public URI logs() throws IOException {
        return github().actions()
                .workflowJobs()
                .downloadJobLogsForWorkflowRun(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubJob response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
