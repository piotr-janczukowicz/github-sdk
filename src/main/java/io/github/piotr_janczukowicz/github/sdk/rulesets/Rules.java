package io.github.piotr_janczukowicz.github.sdk.rulesets;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleCreation;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleCreationType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDeletion;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDeletionType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleNonFastForward;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleNonFastForwardType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleParamsStatusCheckConfiguration;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulePullRequest;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulePullRequestParameters;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulePullRequestType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredLinearHistory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredLinearHistoryType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredSignatures;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredSignaturesType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredStatusChecks;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredStatusChecksParameters;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredStatusChecksType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleUpdate;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleUpdateType;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import java.util.List;

/**
 * Builds the rules that are used most often, as the own records of the client. Use them with
 * {@link RulesetDraft#rule(GitHubRepositoryRule)} and {@link RulesetUpdate#addRule(GitHubRepositoryRule)}. A rule type
 * that is not here is built directly from its client record.
 */
public final class Rules {

    private static final int MAX_APPROVALS = 10;

    private Rules() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Only actors who may bypass can create matching refs.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule restrictCreation() {
        return new GitHubRepositoryRuleCreation(GitHubRepositoryRuleCreationType.CREATION);
    }

    /**
     * Only actors who may bypass can delete matching refs.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule restrictDeletion() {
        return new GitHubRepositoryRuleDeletion(GitHubRepositoryRuleDeletionType.DELETION);
    }

    /**
     * Only actors who may bypass can update matching refs.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule restrictUpdates() {
        return new GitHubRepositoryRuleUpdate(GitHubRepositoryRuleUpdateType.UPDATE, null);
    }

    /**
     * Blocks force pushes to matching refs.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule blockForcePushes() {
        return new GitHubRepositoryRuleNonFastForward(GitHubRepositoryRuleNonFastForwardType.NON_FAST_FORWARD);
    }

    /**
     * Refuses merge commits, so the history of matching refs stays linear.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule requireLinearHistory() {
        return new GitHubRepositoryRuleRequiredLinearHistory(
                GitHubRepositoryRuleRequiredLinearHistoryType.REQUIRED_LINEAR_HISTORY);
    }

    /**
     * Requires signed commits on matching refs.
     *
     * @return the rule
     */
    public static GitHubRepositoryRule requireSignedCommits() {
        return new GitHubRepositoryRuleRequiredSignatures(
                GitHubRepositoryRuleRequiredSignaturesType.REQUIRED_SIGNATURES);
    }

    /**
     * Requires a pull request with approving reviews, with no further review options.
     *
     * @param approvals the number of approving reviews, from 0 to 10
     * @return the rule
     * @throws IllegalArgumentException if {@code approvals} is outside 0 to 10
     */
    public static GitHubRepositoryRule requirePullRequest(final int approvals) {
        return requirePullRequest(approvals, false, false, false, false);
    }

    /**
     * Requires a pull request with approving reviews.
     *
     * @param approvals the number of approving reviews, from 0 to 10
     * @param isStaleDismissed {@code true} to dismiss approvals when new commits are pushed
     * @param isCodeOwnerRequired {@code true} to require a review from the code owners
     * @param isLastPushApprovalRequired {@code true} to require that someone other than the last pusher approves
     * @param isConversationResolutionRequired {@code true} to require all conversations to be resolved
     * @return the rule
     * @throws IllegalArgumentException if {@code approvals} is outside 0 to 10
     */
    public static GitHubRepositoryRule requirePullRequest(
            final int approvals,
            final boolean isStaleDismissed,
            final boolean isCodeOwnerRequired,
            final boolean isLastPushApprovalRequired,
            final boolean isConversationResolutionRequired) {
        if (approvals < 0 || approvals > MAX_APPROVALS) {
            throw new IllegalArgumentException("approvals must be from 0 to " + MAX_APPROVALS + ": " + approvals);
        }
        return new GitHubRepositoryRulePullRequest(
                GitHubRepositoryRulePullRequestType.PULL_REQUEST,
                new GitHubRepositoryRulePullRequestParameters(
                        null,
                        isStaleDismissed,
                        null,
                        isCodeOwnerRequired,
                        isLastPushApprovalRequired,
                        approvals,
                        isConversationResolutionRequired,
                        null,
                        null));
    }

    /**
     * Requires status checks to pass before matching refs can be updated.
     *
     * @param isStrict {@code true} to also require the branch to be up to date with the base branch
     * @param contexts the names of the checks that must pass
     * @return the rule
     * @throws NullPointerException if {@code contexts} or a context is {@code null}
     * @throws IllegalArgumentException if a context is blank
     */
    public static GitHubRepositoryRule requireStatusChecks(final boolean isStrict, final List<String> contexts) {
        final List<GitHubRepositoryRuleParamsStatusCheckConfiguration> checks =
                Arguments.requireNotBlankElements(contexts, "contexts").stream()
                        .map(context -> new GitHubRepositoryRuleParamsStatusCheckConfiguration(context, null))
                        .toList();
        return new GitHubRepositoryRuleRequiredStatusChecks(
                GitHubRepositoryRuleRequiredStatusChecksType.REQUIRED_STATUS_CHECKS,
                new GitHubRepositoryRuleRequiredStatusChecksParameters(null, checks, isStrict));
    }
}
