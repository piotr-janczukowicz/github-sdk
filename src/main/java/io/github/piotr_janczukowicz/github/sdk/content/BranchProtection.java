package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtectionSetting;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.OptionalInt;

/**
 * The protection rule of a {@link Branch}, as GitHub returned it. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover. Change a rule with
 * {@link Branch#protect()}.
 */
public final class BranchProtection {

    private final Branch branch;
    private final GitHubBranchProtection response;

    public BranchProtection(final Branch branch, final GitHubBranchProtection response) {
        this.branch = requireNonNull(branch, "branch");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the branch the rule protects.
     *
     * @return the branch
     */
    public Branch branch() {
        return branch;
    }

    /**
     * Tells whether the rule also applies to administrators.
     *
     * @return {@code true} when administrators are subject to the rule
     */
    public boolean isEnforcedForAdmins() {
        return response.enforceAdmins() != null && response.enforceAdmins().enabled();
    }

    /**
     * Returns the number of approving reviews a pull request needs before it can be merged.
     *
     * @return the number, or empty when the rule does not require reviews
     */
    public OptionalInt requiredApprovals() {
        if (response.requiredPullRequestReviews() == null
                || response.requiredPullRequestReviews().requiredApprovingReviewCount() == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(response.requiredPullRequestReviews().requiredApprovingReviewCount());
    }

    /**
     * Tells whether merge commits are refused, so the history stays linear.
     *
     * @return {@code true} when a linear history is required
     */
    public boolean isLinearHistoryRequired() {
        return isEnabled(response.requiredLinearHistory());
    }

    /**
     * Tells whether force pushes are allowed.
     *
     * @return {@code true} when everyone with push access may force push
     */
    public boolean allowsForcePushes() {
        return isEnabled(response.allowForcePushes());
    }

    /**
     * Tells whether the branch may be deleted.
     *
     * @return {@code true} when deleting is allowed
     */
    public boolean allowsDeletions() {
        return isEnabled(response.allowDeletions());
    }

    /**
     * Loads the rule again ({@code GET /repos/{owner}/{repo}/branches/{branch}/protection}). This object is not
     * changed.
     *
     * @return the rule as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the branch has no rule any more
     */
    public BranchProtection refresh() throws IOException {
        return branch.protection()
                .orElseThrow(() -> new IllegalStateException("Branch " + branch.name() + " has no protection"));
    }

    /**
     * Removes the rule ({@code DELETE /repos/{owner}/{repo}/branches/{branch}/protection}). The branch stays.
     *
     * @throws IOException if the transport fails
     */
    public void remove() throws IOException {
        final GitHub github = Internals.github(branch.repository());
        github.branches()
                .branchProtection()
                .deleteBranchProtection(
                        branch.repository().owner(), branch.repository().name(), branch.name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubBranchProtection response() {
        return response;
    }

    private static boolean isEnabled(final GitHubBranchProtectionSetting setting) {
        return setting != null && Boolean.TRUE.equals(setting.enabled());
    }
}
