package io.github.piotr_janczukowicz.github.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateMilestonePayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneState;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A milestone of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives
 * the complete response for every field the accessors here do not cover.
 */
public final class Milestone {

    private final Repository repository;
    private final GitHubMilestone response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Milestone(final Repository repository, final GitHubMilestone response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the milestone belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the milestone number within its repository.
     *
     * @return the number
     */
    public int number() {
        return response.number();
    }

    /**
     * Returns the milestone title.
     *
     * @return the title
     */
    public String title() {
        return response.title();
    }

    /**
     * Returns the milestone description.
     *
     * @return the description, or empty when the milestone has none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Deletes the milestone ({@code DELETE /repos/{owner}/{repo}/milestones/{milestone_number}}). GitHub removes it
     * from every issue.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.github(repository)
                .issues()
                .milestones()
                .deleteMilestone(repository.owner(), repository.name(), number());
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link MilestoneUpdate#send()}.
     *
     * @return a new update for this milestone
     */
    public MilestoneUpdate update() {
        return new MilestoneUpdate(this);
    }

    /**
     * Closes the milestone ({@code PATCH /repos/{owner}/{repo}/milestones/{milestone_number}}). This object is not
     * changed.
     *
     * @return the milestone as GitHub returned it after the change
     * @throws IOException if the transport fails
     */
    public Milestone close() throws IOException {
        return apply(new GitHubUpdateMilestonePayload(null, GitHubMilestoneState.CLOSED, null, null));
    }

    /**
     * Reopens the milestone ({@code PATCH /repos/{owner}/{repo}/milestones/{milestone_number}}). This object is not
     * changed.
     *
     * @return the milestone as GitHub returned it after the change
     * @throws IOException if the transport fails
     */
    public Milestone reopen() throws IOException {
        return apply(new GitHubUpdateMilestonePayload(null, GitHubMilestoneState.OPEN, null, null));
    }

    /**
     * Loads the milestone again ({@code GET /repos/{owner}/{repo}/milestones/{milestone_number}}). This object is not
     * changed.
     *
     * @return the milestone as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the milestone is gone
     */
    public Milestone refresh() throws IOException {
        return repository
                .milestone(number())
                .orElseThrow(() -> new IllegalStateException("Milestone " + title() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubMilestone response() {
        return response;
    }

    Milestone apply(final GitHubUpdateMilestonePayload payload) throws IOException {
        return new Milestone(
                repository,
                Internals.github(repository)
                        .issues()
                        .milestones()
                        .updateMilestone(repository.owner(), repository.name(), number(), payload));
    }
}
