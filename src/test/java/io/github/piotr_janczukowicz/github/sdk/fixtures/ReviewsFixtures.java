package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReview;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewComment;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewCommentBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewCommentLinks;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewLinks;
import io.github.piotr_janczukowicz.github.api.search.GitHubAuthorAssociation;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.time.Instant;

public final class ReviewsFixtures {

    public static final long REVIEW_ID = 80L;
    public static final long COMMENT_ID = 91L;

    private static final String API = "https://api.github.com/repos/octocat/hello-world/pulls/7";
    private static final Instant CREATED_AT = Instant.parse("2026-02-03T04:05:06Z");

    private ReviewsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubPullRequestReview review() {
        return GitHubPullRequestReviewBuilder.builder()
                .id(REVIEW_ID)
                .nodeId("PRR_kwDOA")
                .user(mock(GitHubSimpleUser.class))
                .body("Looks good")
                .state("APPROVED")
                .htmlUrl("https://github.com/octocat/hello-world/pull/7#pullrequestreview-80")
                .pullRequestUrl(API)
                .links(mock(GitHubPullRequestReviewLinks.class))
                .submittedAt(CREATED_AT)
                .commitId("abc123")
                .authorAssociation(GitHubAuthorAssociation.COLLABORATOR)
                .build();
    }

    public static GitHubPullRequestReview reviewInState(final String state) {
        return GitHubPullRequestReviewBuilder.builder(review()).state(state).build();
    }

    public static GitHubPullRequestReviewComment comment() {
        return GitHubPullRequestReviewCommentBuilder.builder()
                .url(API + "/comments/91")
                .pullRequestReviewId(REVIEW_ID)
                .id(COMMENT_ID)
                .nodeId("PRRC_kwDOA")
                .diffHunk("@@ -1 +1 @@")
                .path("src/Main.java")
                .commitId("abc123")
                .originalCommitId("abc123")
                .user(OptionalNull.of(mock(GitHubSimpleUser.class)))
                .body("Rename this")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .htmlUrl("https://github.com/octocat/hello-world/pull/7#discussion_r91")
                .pullRequestUrl(API)
                .authorAssociation(GitHubAuthorAssociation.COLLABORATOR)
                .links(mock(GitHubPullRequestReviewCommentLinks.class))
                .line(12)
                .build();
    }
}
