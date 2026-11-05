package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireBetween;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubUpdateBranchProtectionPayload;
import io.github.piotr_janczukowicz.github.api.branches.GitHubUpdateBranchProtectionRequiredPullRequestReviews;
import io.github.piotr_janczukowicz.github.api.branches.GitHubUpdateBranchProtectionRequiredStatusChecks;
import io.github.piotr_janczukowicz.github.api.branches.GitHubUpdateBranchProtectionRestrictions;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Collects the settings of a protection rule for a {@link Branch} and sends them in one request. GitHub replaces the
 * whole rule on every request, so a setting that was not chosen here is switched off, whatever the branch had before.
 * Nothing is sent until {@link #send()}, and sending without any setting protects the branch with every option off.
 */
public final class BranchProtectionUpdate {

    private static final int MIN_APPROVALS = 0;
    private static final int MAX_APPROVALS = 6;

    private final Branch branch;
    private @Nullable GitHubUpdateBranchProtectionRequiredStatusChecks statusChecks;
    private @Nullable GitHubUpdateBranchProtectionRequiredPullRequestReviews pullRequestReviews;
    private @Nullable GitHubUpdateBranchProtectionRestrictions restrictions;
    private boolean isAdminsEnforced;
    private boolean isLinearHistoryRequired;
    private boolean isForcePushAllowed;
    private boolean isDeletionAllowed;
    private boolean isCreationBlocked;
    private boolean isConversationResolutionRequired;
    private boolean isLocked;
    private boolean isForkSyncingAllowed;

    BranchProtectionUpdate(final Branch branch) {
        this.branch = branch;
    }

    /**
     * Requires status checks to pass before a pull request can be merged.
     *
     * @param isStrict {@code true} to also require the pull request branch to be up to date with the base branch
     * @param contexts the names of the checks that must pass
     * @return this update
     * @throws NullPointerException if {@code contexts} or a context is {@code null}
     * @throws IllegalArgumentException if a context is blank
     */
    public BranchProtectionUpdate requireStatusChecks(final boolean isStrict, final List<String> contexts) {
        this.statusChecks = new GitHubUpdateBranchProtectionRequiredStatusChecks(
                isStrict, Arguments.requireNotBlankElements(contexts, "contexts"), null);
        return this;
    }

    /**
     * Requires approving reviews before a pull request can be merged, with no further review options.
     *
     * @param approvals the number of approving reviews, from 0 to 6
     * @return this update
     * @throws IllegalArgumentException if {@code approvals} is not from 0 to 6
     */
    public BranchProtectionUpdate requirePullRequestReviews(final int approvals) {
        return requirePullRequestReviews(approvals, false, false, false);
    }

    /**
     * Requires approving reviews before a pull request can be merged.
     *
     * @param approvals the number of approving reviews, from 0 to 6
     * @param isStaleDismissed {@code true} to dismiss approvals when new commits are pushed
     * @param isCodeOwnerRequired {@code true} to require a review from the code owners
     * @param isLastPushApprovalRequired {@code true} to require that someone other than the last pusher approves
     * @return this update
     * @throws IllegalArgumentException if {@code approvals} is not from 0 to 6
     */
    public BranchProtectionUpdate requirePullRequestReviews(
            final int approvals,
            final boolean isStaleDismissed,
            final boolean isCodeOwnerRequired,
            final boolean isLastPushApprovalRequired) {
        requireBetween(approvals, MIN_APPROVALS, MAX_APPROVALS, "approvals");
        this.pullRequestReviews = new GitHubUpdateBranchProtectionRequiredPullRequestReviews(
                null, isStaleDismissed, isCodeOwnerRequired, approvals, isLastPushApprovalRequired, null);
        return this;
    }

    /**
     * Applies the rule to administrators too.
     *
     * @return this update
     */
    public BranchProtectionUpdate enforceAdmins() {
        this.isAdminsEnforced = true;
        return this;
    }

    /**
     * Refuses merge commits, so the history stays linear.
     *
     * @return this update
     */
    public BranchProtectionUpdate requireLinearHistory() {
        this.isLinearHistoryRequired = true;
        return this;
    }

    /**
     * Allows everyone with push access to force push.
     *
     * @return this update
     */
    public BranchProtectionUpdate allowForcePushes() {
        this.isForcePushAllowed = true;
        return this;
    }

    /**
     * Allows deleting the branch.
     *
     * @return this update
     */
    public BranchProtectionUpdate allowDeletions() {
        this.isDeletionAllowed = true;
        return this;
    }

    /**
     * Blocks creating matching branches. It only takes effect together with push restrictions.
     *
     * @return this update
     */
    public BranchProtectionUpdate blockCreations() {
        this.isCreationBlocked = true;
        return this;
    }

    /**
     * Requires all conversations on a pull request to be resolved before it can be merged.
     *
     * @return this update
     */
    public BranchProtectionUpdate requireConversationResolution() {
        this.isConversationResolutionRequired = true;
        return this;
    }

    /**
     * Makes the branch read-only.
     *
     * @return this update
     */
    public BranchProtectionUpdate lockBranch() {
        this.isLocked = true;
        return this;
    }

    /**
     * Lets a fork of the repository sync the branch from upstream, which only matters for a locked branch.
     *
     * @return this update
     */
    public BranchProtectionUpdate allowForkSyncing() {
        this.isForkSyncingAllowed = true;
        return this;
    }

    /**
     * Restricts who can push to the branch. Only organization repositories support it.
     *
     * @param users the logins of the users who may push
     * @param teams the slugs of the teams who may push
     * @param apps the slugs of the apps that may push
     * @return this update
     * @throws NullPointerException if {@code users}, {@code teams}, {@code apps} or one of their entries is
     *     {@code null}
     * @throws IllegalArgumentException if an entry is blank
     */
    public BranchProtectionUpdate restrictPushesTo(
            final List<String> users, final List<String> teams, final List<String> apps) {
        this.restrictions = new GitHubUpdateBranchProtectionRestrictions(
                Arguments.requireNotBlankElements(users, "users"),
                Arguments.requireNotBlankElements(teams, "teams"),
                Arguments.requireNotBlankElements(apps, "apps"));
        return this;
    }

    /**
     * Sends the rule ({@code PUT /repos/{owner}/{repo}/branches/{branch}/protection}) and loads it again. The branch
     * this update was made from is not changed.
     *
     * @return the rule as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not report the rule it just set
     */
    public BranchProtection send() throws IOException {
        final Repository repository = branch.repository();
        final GitHub github = Internals.github(repository);
        github.branches()
                .branchProtection()
                .updateBranchProtection(repository.owner(), repository.name(), branch.name(), payload());
        return branch.protection()
                .orElseThrow(() -> new IllegalStateException("Branch " + branch.name() + " has no protection"));
    }

    private GitHubUpdateBranchProtectionPayload payload() {
        return new GitHubUpdateBranchProtectionPayload(
                optional(statusChecks),
                OptionalNull.of(isAdminsEnforced),
                optional(pullRequestReviews),
                optional(restrictions),
                isLinearHistoryRequired,
                OptionalNull.of(isForcePushAllowed),
                isDeletionAllowed,
                isCreationBlocked,
                isConversationResolutionRequired,
                isLocked,
                isForkSyncingAllowed);
    }

    private static <T> OptionalNull<T> optional(final @Nullable T value) {
        return value == null ? OptionalNull.ofNull() : OptionalNull.of(value);
    }
}
