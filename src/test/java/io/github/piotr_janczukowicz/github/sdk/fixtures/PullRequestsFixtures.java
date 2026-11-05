package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequest;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestBranchRef;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestSimpleLinks;
import io.github.piotr_janczukowicz.github.api.search.GitHubAuthorAssociation;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;
import java.util.List;

public final class PullRequestsFixtures {

    private static final String API = "https://api.github.com/repos/octocat/hello-world/pulls/7";
    private static final Instant CREATED_AT = Instant.parse("2026-02-03T04:05:06Z");

    private PullRequestsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubPullRequest pullRequest() {
        return GitHubPullRequestBuilder.builder()
                .url(API)
                .id(3003L)
                .nodeId("PR_kwDOA")
                .htmlUrl("https://github.com/octocat/hello-world/pull/7")
                .diffUrl("https://github.com/octocat/hello-world/pull/7.diff")
                .patchUrl("https://github.com/octocat/hello-world/pull/7.patch")
                .issueUrl("https://api.github.com/repos/octocat/hello-world/issues/7")
                .commitsUrl(API + "/commits")
                .reviewCommentsUrl(API + "/comments")
                .reviewCommentUrl(API + "/comments{/number}")
                .commentsUrl("https://api.github.com/repos/octocat/hello-world/issues/7/comments")
                .statusesUrl("https://api.github.com/repos/octocat/hello-world/statuses/abc123")
                .number(7)
                .state(GitHubPullRequestUpdateState.OPEN)
                .locked(false)
                .title("Add feature")
                .user(mock(GitHubSimpleUser.class))
                .labels(List.of())
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .head(mock(GitHubPullRequestBranchRef.class))
                .base(mock(GitHubPullRequestBranchRef.class))
                .links(mock(GitHubPullRequestSimpleLinks.class))
                .authorAssociation(GitHubAuthorAssociation.COLLABORATOR)
                .merged(false)
                .mergeableState("clean")
                .comments(0)
                .reviewComments(0)
                .maintainerCanModify(true)
                .commits(1)
                .additions(1)
                .deletions(0)
                .changedFiles(1)
                .build();
    }
}
