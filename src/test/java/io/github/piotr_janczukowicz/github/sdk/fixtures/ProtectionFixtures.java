package io.github.piotr_janczukowicz.github.sdk.fixtures;

import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtectionBuilder;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtectionSetting;
import io.github.piotr_janczukowicz.github.api.branches.GitHubProtectedBranchAdminEnforced;
import io.github.piotr_janczukowicz.github.api.branches.GitHubProtectedBranchPullRequestReview;
import io.github.piotr_janczukowicz.github.api.branches.GitHubProtectedBranchPullRequestReviewBuilder;

public final class ProtectionFixtures {

    public static final int APPROVALS = 2;

    private static final String URL = "https://api.github.com/repos/octocat/hello-world/branches/main/protection";

    private ProtectionFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubBranchProtection protection() {
        return GitHubBranchProtectionBuilder.builder()
                .url(URL)
                .enforceAdmins(new GitHubProtectedBranchAdminEnforced(URL + "/enforce_admins", true))
                .requiredPullRequestReviews(reviews())
                .requiredLinearHistory(new GitHubBranchProtectionSetting(true))
                .allowForcePushes(new GitHubBranchProtectionSetting(false))
                .allowDeletions(new GitHubBranchProtectionSetting(true))
                .build();
    }

    public static GitHubBranchProtection bareProtection() {
        return GitHubBranchProtectionBuilder.builder().url(URL).build();
    }

    public static GitHubBranchProtection protectionWithoutApprovalCount() {
        return GitHubBranchProtectionBuilder.builder(protection())
                .requiredPullRequestReviews(GitHubProtectedBranchPullRequestReviewBuilder.builder()
                        .url(URL + "/required_pull_request_reviews")
                        .build())
                .build();
    }

    private static GitHubProtectedBranchPullRequestReview reviews() {
        return GitHubProtectedBranchPullRequestReviewBuilder.builder()
                .url(URL + "/required_pull_request_reviews")
                .requiredApprovingReviewCount(APPROVALS)
                .build();
    }
}
