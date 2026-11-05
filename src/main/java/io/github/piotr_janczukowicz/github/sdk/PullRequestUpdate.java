package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link PullRequest} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class PullRequestUpdate {

    private final PullRequest pullRequest;
    private @Nullable String title;
    private @Nullable String body;
    private @Nullable String base;
    private @Nullable Boolean maintainerCanModify;
    private final UpdateState state = new UpdateState();

    PullRequestUpdate(final PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    /**
     * Sets the title.
     *
     * @param title the new title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     */
    public PullRequestUpdate title(final String title) {
        this.title = requireNonNull(title, "title");
        return changed();
    }

    /**
     * Sets the body.
     *
     * @param body the new body in Markdown
     * @return this update
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public PullRequestUpdate body(final String body) {
        this.body = requireNonNull(body, "body");
        return changed();
    }

    /**
     * Sets the base branch.
     *
     * @param base the name of the branch to merge into
     * @return this update
     * @throws NullPointerException if {@code base} is {@code null}
     */
    public PullRequestUpdate base(final String base) {
        this.base = requireNonNull(base, "base");
        return changed();
    }

    /**
     * Sets whether maintainers may modify the pull request.
     *
     * @param allowed {@code true} to allow it
     * @return this update
     */
    public PullRequestUpdate maintainerCanModify(final boolean allowed) {
        this.maintainerCanModify = allowed;
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}/pulls/{pull_number}}). The pull request this update was
     * made from is not changed.
     *
     * @return the pull request as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public PullRequest send() throws IOException {
        state.requireChange();
        return pullRequest.apply(new GitHubUpdatePullRequestPayload(title, body, null, base, maintainerCanModify));
    }

    private PullRequestUpdate changed() {
        state.mark();
        return this;
    }
}
