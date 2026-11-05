package io.github.piotr_janczukowicz.github.sdk.fixtures;

import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueComment;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueCommentBuilder;
import java.time.Instant;
import java.util.List;

public final class IssuesFixtures {

    private static final String API = "https://api.github.com/repos/octocat/hello-world/issues/42";
    private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    private IssuesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubIssue issue() {
        return GitHubIssueBuilder.builder()
                .url(API)
                .repositoryUrl("https://api.github.com/repos/octocat/hello-world")
                .labelsUrl(API + "/labels{/name}")
                .commentsUrl(API + "/comments")
                .eventsUrl(API + "/events")
                .htmlUrl("https://github.com/octocat/hello-world/issues/42")
                .id(1001L)
                .nodeId("I_kwDOA")
                .number(42)
                .title("Found a bug")
                .locked(false)
                .labels(List.of())
                .state("open")
                .comments(0)
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    public static GitHubIssueComment issueComment() {
        return GitHubIssueCommentBuilder.builder()
                .id(2002L)
                .nodeId("IC_kwDOA")
                .url("https://api.github.com/repos/octocat/hello-world/issues/comments/2002")
                .body("First comment")
                .htmlUrl("https://github.com/octocat/hello-world/issues/42#issuecomment-2002")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .issueUrl(API)
                .build();
    }
}
