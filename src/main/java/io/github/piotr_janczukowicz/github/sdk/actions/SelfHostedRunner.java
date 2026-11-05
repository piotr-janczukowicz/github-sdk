package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabel;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabels;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import java.io.IOException;
import java.util.List;

/**
 * A self-hosted runner of a repository or organization. The object is immutable, and {@link #response()} gives the
 * complete response. The methods that change labels return what GitHub holds afterwards and do not change this object.
 */
public final class SelfHostedRunner {

    private final SelfHostedRunners runners;
    private final GitHubRunner response;

    SelfHostedRunner(final SelfHostedRunners runners, final GitHubRunner response) {
        this.runners = requireNonNull(runners, "runners");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the runner id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the runner name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the operating system of the runner.
     *
     * @return the operating system
     */
    public String os() {
        return response.os();
    }

    /**
     * Returns the status of the runner, {@code online} or {@code offline}.
     *
     * @return the status
     */
    public String status() {
        return response.status();
    }

    /**
     * Tells whether the runner is running a job right now.
     *
     * @return {@code true} when busy
     */
    public boolean isBusy() {
        return response.busy();
    }

    /**
     * Returns the labels of the runner, built in and custom.
     *
     * @return the labels
     */
    public List<GitHubRunnerLabel> labels() {
        return response.labels();
    }

    /**
     * Loads the runner again ({@code GET .../actions/runners/{runner_id}}). This object is not changed.
     *
     * @return the runner as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the runner is gone
     */
    public SelfHostedRunner refresh() throws IOException {
        return runners.find(id()).orElseThrow(() -> new IllegalStateException("Runner " + id() + " is gone"));
    }

    /**
     * Adds custom labels ({@code POST .../actions/runners/{runner_id}/labels}).
     *
     * @param labels the labels to add, at least one
     * @return the labels of the runner afterwards
     * @throws NullPointerException if {@code labels} or a label is {@code null}
     * @throws IllegalArgumentException if {@code labels} is empty or a label is blank
     * @throws IOException if the transport fails
     */
    public GitHubRunnerLabels addLabels(final List<String> labels) throws IOException {
        return runners.gateway().addLabels(id(), Arguments.requireNotBlankValues(labels, "labels"));
    }

    /**
     * Replaces all custom labels ({@code PUT .../actions/runners/{runner_id}/labels}).
     *
     * @param labels the new custom labels; an empty list removes all of them
     * @return the labels of the runner afterwards
     * @throws NullPointerException if {@code labels} or a label is {@code null}
     * @throws IllegalArgumentException if a label is blank
     * @throws IOException if the transport fails
     */
    public GitHubRunnerLabels replaceLabels(final List<String> labels) throws IOException {
        return runners.gateway().replaceLabels(id(), Arguments.requireNotBlankElements(labels, "labels"));
    }

    /**
     * Removes one custom label ({@code DELETE .../actions/runners/{runner_id}/labels/{name}}).
     *
     * @param label the label to remove
     * @return the labels of the runner afterwards
     * @throws NullPointerException if {@code label} is {@code null}
     * @throws IllegalArgumentException if {@code label} is blank
     * @throws IOException if the transport fails
     */
    public GitHubRunnerLabels removeLabel(final String label) throws IOException {
        return runners.gateway().removeLabel(id(), requireNotBlank(label, "label"));
    }

    /**
     * Removes all custom labels ({@code DELETE .../actions/runners/{runner_id}/labels}).
     *
     * @return the labels of the runner afterwards
     * @throws IOException if the transport fails
     */
    public GitHubRunnerLabels removeAllLabels() throws IOException {
        return runners.gateway().removeAllLabels(id());
    }

    /**
     * Removes the runner from GitHub ({@code DELETE .../actions/runners/{runner_id}}). GitHub refuses it while the
     * runner is running a job.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        runners.gateway().delete(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubRunner response() {
        return response;
    }
}
