package io.github.piotr_janczukowicz.github.sdk.checks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunOutput;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunPayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunStatus;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link CheckRun} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. Annotations, images and actions are not part of it; send them with
 * {@link CheckRun#update(GitHubUpdateCheckRunPayload)}. GitHub lets only GitHub Apps change check runs.
 */
public final class CheckRunUpdate {

    private final CheckRun checkRun;
    private @Nullable String name;
    private @Nullable String detailsUrl;
    private @Nullable String externalId;
    private @Nullable Instant startedAt;
    private @Nullable GitHubUpdateCheckRunStatus status;
    private @Nullable GitHubUpdateCheckRunConclusion conclusion;
    private @Nullable Instant completedAt;
    private @Nullable GitHubUpdateCheckRunOutput output;
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param checkRun the check run
     * @throws NullPointerException if {@code checkRun} is {@code null}
     */
    public CheckRunUpdate(final CheckRun checkRun) {
        this.checkRun = requireNonNull(checkRun, "checkRun");
    }

    /**
     * Sets the new name.
     *
     * @param name the new value
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public CheckRunUpdate name(final String name) {
        this.name = requireNotBlank(name, "name");
        return changed();
    }

    /**
     * Sets the link to the details of the run on your own site.
     *
     * @param detailsUrl the new value
     * @return this update
     * @throws NullPointerException if {@code detailsUrl} is {@code null}
     */
    public CheckRunUpdate detailsUrl(final String detailsUrl) {
        this.detailsUrl = requireNonNull(detailsUrl, "detailsUrl");
        return changed();
    }

    /**
     * Sets the reference of the run in your own system.
     *
     * @param externalId the new value
     * @return this update
     * @throws NullPointerException if {@code externalId} is {@code null}
     */
    public CheckRunUpdate externalId(final String externalId) {
        this.externalId = requireNonNull(externalId, "externalId");
        return changed();
    }

    /**
     * Sets when the run started.
     *
     * @param startedAt the new value
     * @return this update
     * @throws NullPointerException if {@code startedAt} is {@code null}
     */
    public CheckRunUpdate startedAt(final Instant startedAt) {
        this.startedAt = requireNonNull(startedAt, "startedAt");
        return changed();
    }

    /**
     * Sets the status. A {@code COMPLETED} status needs a conclusion, which {@link #send()} checks.
     *
     * @param status the new value
     * @return this update
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public CheckRunUpdate status(final GitHubUpdateCheckRunStatus status) {
        this.status = requireNonNull(status, "status");
        return changed();
    }

    /**
     * Sets the conclusion, for a run that is completed.
     *
     * @param conclusion the new value
     * @return this update
     * @throws NullPointerException if {@code conclusion} is {@code null}
     */
    public CheckRunUpdate conclusion(final GitHubUpdateCheckRunConclusion conclusion) {
        this.conclusion = requireNonNull(conclusion, "conclusion");
        return changed();
    }

    /**
     * Sets when the run completed.
     *
     * @param completedAt the new value
     * @return this update
     * @throws NullPointerException if {@code completedAt} is {@code null}
     */
    public CheckRunUpdate completedAt(final Instant completedAt) {
        this.completedAt = requireNonNull(completedAt, "completedAt");
        return changed();
    }

    /**
     * Sets the output of the run: a title and a summary in Markdown, up to 65,535 characters.
     *
     * @param title the title
     * @param summary the summary
     * @return this update
     * @throws NullPointerException if {@code title} or {@code summary} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code summary} is blank, or {@code summary} is too long
     */
    public CheckRunUpdate output(final String title, final String summary) {
        this.output = new GitHubUpdateCheckRunOutput(
                requireNotBlank(title, "title"), requireNotBlank(summary, "summary"), null, null, null);
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}/check-runs/{check_run_id}}). The object this update was
     * made from is not changed.
     *
     * @return the check run as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IllegalArgumentException if the status is {@code COMPLETED} and no conclusion was set
     * @throws IOException if the transport fails
     */
    public CheckRun send() throws IOException {
        state.requireChange();
        return checkRun.apply(new GitHubUpdateCheckRunPayload(
                name, detailsUrl, externalId, startedAt, status, conclusion, completedAt, output, null));
    }

    private CheckRunUpdate changed() {
        state.mark();
        return this;
    }
}
