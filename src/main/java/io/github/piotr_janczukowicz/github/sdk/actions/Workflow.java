package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateWorkflowDispatchPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListWorkflowRunsFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflow;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowState;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A workflow of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Workflow {

    private static final String REF = "ref";

    private final Repository repository;
    private final GitHubWorkflow response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Workflow(final Repository repository, final GitHubWorkflow response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the workflow belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the workflow id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the name of the workflow.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the path of the workflow file, for example {@code .github/workflows/build.yml}.
     *
     * @return the path
     */
    public String path() {
        return response.path();
    }

    /**
     * Returns the state of the workflow, as the client decoded it. The client maps a value it does not know to its own
     * {@code UNRECOGNIZED} constant.
     *
     * @return the state
     */
    public GitHubWorkflowState state() {
        return response.state();
    }

    /**
     * Tells whether the workflow is enabled.
     *
     * @return {@code true} when the state is {@code ACTIVE}
     */
    public boolean isActive() {
        return response.state() == GitHubWorkflowState.ACTIVE;
    }

    /**
     * Loads the workflow again ({@code GET /repos/{owner}/{repo}/actions/workflows/{workflow_id}}). This object is not
     * changed.
     *
     * @return the workflow as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the workflow is gone
     */
    public Workflow refresh() throws IOException {
        return github().actions()
                .workflows()
                .getWorkflow(repository.owner(), repository.name(), key())
                .map(reloaded -> new Workflow(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Workflow " + id() + " is gone"));
    }

    /**
     * Enables the workflow ({@code PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/enable}). This object is
     * not changed.
     *
     * @return the workflow as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the workflow is gone
     */
    public Workflow enable() throws IOException {
        github().actions().workflows().enableWorkflow(repository.owner(), repository.name(), key());
        return refresh();
    }

    /**
     * Disables the workflow ({@code PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/disable}). This object is
     * not changed.
     *
     * @return the workflow as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the workflow is gone
     */
    public Workflow disable() throws IOException {
        github().actions().workflows().disableWorkflow(repository.owner(), repository.name(), key());
        return refresh();
    }

    /**
     * Starts the workflow without inputs. See {@link #dispatch(String, Map)}.
     *
     * @param ref the branch or tag to run on
     * @return the run that was started
     * @throws NullPointerException if {@code ref} is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the run it reported
     */
    public WorkflowRun dispatch(final String ref) throws IOException {
        return dispatch(ref, Map.of());
    }

    /**
     * Starts the workflow ({@code POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches}). The workflow
     * file must declare the {@code workflow_dispatch} trigger.
     *
     * @param ref the branch or tag to run on
     * @param inputs the workflow inputs by name
     * @return the run that was started
     * @throws NullPointerException if {@code ref} or {@code inputs} is {@code null}, or an input value is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the run it reported
     */
    public WorkflowRun dispatch(final String ref, final Map<String, Object> inputs) throws IOException {
        requireNonNull(inputs, "inputs").forEach((key, value) -> {
            if (value == null) {
                throw new NullPointerException("inputs[" + key + "]");
            }
        });
        final GitHubCreateWorkflowDispatchPayload payload =
                new GitHubCreateWorkflowDispatchPayload(requireNotBlank(ref, REF), Map.copyOf(inputs));
        final long runId = github().actions()
                .workflows()
                .createWorkflowDispatch(repository.owner(), repository.name(), key(), payload)
                .workflowRunId()
                .value();
        return github().actions()
                .workflowRuns()
                .getWorkflowRun(repository.owner(), repository.name(), runId)
                .map(started -> new WorkflowRun(repository, started))
                .orElseThrow(() -> new IllegalStateException("Workflow run " + runId + " is gone"));
    }

    /**
     * Lists the runs of the workflow. See {@link #runs(GitHubListWorkflowRunsFilter)}.
     *
     * @return the runs, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowRun> runs() throws IOException {
        return runs(GitHubListWorkflowRunsFilter.empty());
    }

    /**
     * Lists the runs of the workflow ({@code GET /repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for actor, branch, event, status, creation date and commit
     * @return the runs, newest first
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowRun> runs(final GitHubListWorkflowRunsFilter filter) throws IOException {
        return github()
                .actions()
                .workflowRuns()
                .listWorkflowRuns(
                        repository.owner(),
                        repository.name(),
                        key(),
                        requireNonNull(filter, "filter"),
                        GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new WorkflowRun(repository, listed));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubWorkflow response() {
        return response;
    }

    private String key() {
        return String.valueOf(id());
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
