package io.github.piotr_janczukowicz.github.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueMilestoneReference;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateIssueAssignee;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateIssueLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to an {@link Issue} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class IssueUpdate {

    private final Issue issue;
    private final GitHubUpdatePayloadBuilder payload = GitHubUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param issue the issue the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     */
    public IssueUpdate(final Issue issue) {
        this.issue = requireNonNull(issue, "issue");
    }

    /**
     * Sets the title.
     *
     * @param title the new title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     */
    public IssueUpdate title(final String title) {
        payload.title(OptionalNull.of(requireNonNull(title, "title")));
        return changed();
    }

    /**
     * Sets the body.
     *
     * @param body the new body in Markdown
     * @return this update
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public IssueUpdate body(final String body) {
        payload.body(OptionalNull.of(requireNonNull(body, "body")));
        return changed();
    }

    /**
     * Removes the body.
     *
     * @return this update
     */
    public IssueUpdate clearBody() {
        payload.body(OptionalNull.ofNull());
        return changed();
    }

    /**
     * Sets the milestone.
     *
     * @param number the milestone number
     * @return this update
     */
    public IssueUpdate milestone(final int number) {
        payload.milestone(OptionalNull.of(GitHubIssueMilestoneReference.ofNumber(number)));
        return changed();
    }

    /**
     * Removes the milestone.
     *
     * @return this update
     */
    public IssueUpdate clearMilestone() {
        payload.milestone(OptionalNull.ofNull());
        return changed();
    }

    /**
     * Replaces the labels.
     *
     * @param names the label names
     * @return this update
     * @throws NullPointerException if {@code names} or a name is {@code null}
     * @throws IllegalArgumentException if a name is blank
     */
    public IssueUpdate labels(final List<String> names) {
        payload.labels(Arguments.requireNotBlankElements(names, "names").stream()
                .map(GitHubUpdateIssueLabel::ofName)
                .toList());
        return changed();
    }

    /**
     * Replaces the assignees.
     *
     * @param logins the user logins
     * @return this update
     * @throws NullPointerException if {@code logins} or a login is {@code null}
     * @throws IllegalArgumentException if a login is blank
     */
    public IssueUpdate assignees(final List<String> logins) {
        payload.assignees(Arguments.requireNotBlankElements(logins, "logins").stream()
                .map(GitHubUpdateIssueAssignee::ofLogin)
                .toList());
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}/issues/{issue_number}}) and loads the issue again. The
     * issue this update was made from is not changed.
     *
     * @return the issue as GitHub holds it after the update
     * @throws IllegalStateException if nothing was set, or the issue is gone after the update
     * @throws IOException if the transport fails
     */
    public Issue send() throws IOException {
        state.requireChange();
        return issue.apply(payload.build());
    }

    private IssueUpdate changed() {
        state.mark();
        return this;
    }
}
