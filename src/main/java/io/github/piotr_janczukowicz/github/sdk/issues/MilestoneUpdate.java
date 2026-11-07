package io.github.piotr_janczukowicz.github.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateMilestonePayload;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link Milestone} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class MilestoneUpdate {

    private final Milestone milestone;
    private @Nullable String title;
    private @Nullable String description;
    private @Nullable Instant dueOn;
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param milestone the milestone
     * @throws NullPointerException if {@code milestone} is {@code null}
     */
    public MilestoneUpdate(final Milestone milestone) {
        this.milestone = requireNonNull(milestone, "milestone");
    }

    /**
     * Sets the new title.
     *
     * @param title the new value
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public MilestoneUpdate title(final String title) {
        this.title = requireNotBlank(title, "title");
        return changed();
    }

    /**
     * Sets the new description.
     *
     * @param description the new value
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public MilestoneUpdate description(final String description) {
        this.description = requireNonNull(description, "description");
        return changed();
    }

    /**
     * Sets the new due date.
     *
     * @param dueOn the new value
     * @return this update
     * @throws NullPointerException if {@code dueOn} is {@code null}
     */
    public MilestoneUpdate dueOn(final Instant dueOn) {
        this.dueOn = requireNonNull(dueOn, "dueOn");
        return changed();
    }

    /**
     * Sends the changes. The object this update was made from is not changed.
     *
     * @return the milestone as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Milestone send() throws IOException {
        state.requireChange();
        return milestone.apply(new GitHubUpdateMilestonePayload(title, null, description, dueOn));
    }

    private MilestoneUpdate changed() {
        state.mark();
        return this;
    }
}
