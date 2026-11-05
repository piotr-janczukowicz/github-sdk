package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunBuilder;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunOutput;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCombinedCommitStatusBuilder;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusBuilder;
import io.github.piotr_janczukowicz.github.api.commits.GitHubSimpleCommitStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.util.List;

public final class ChecksFixtures {

    public static final long CHECK_RUN_ID = 5005L;
    public static final String SHA = "abc123";

    private static final String API = "https://api.github.com/repos/octocat/hello-world";

    private ChecksFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubCheckRun checkRun() {
        return GitHubCheckRunBuilder.builder()
                .id(CHECK_RUN_ID)
                .headSha(SHA)
                .nodeId("CR_kwDOA")
                .url(API + "/check-runs/5005")
                .detailsUrl("https://ci.example.com/run/5005")
                .status(GitHubCheckRunStatus.COMPLETED)
                .conclusion(GitHubCheckRunConclusion.SUCCESS)
                .output(mock(GitHubCheckRunOutput.class))
                .name("build")
                .pullRequests(List.of())
                .build();
    }

    public static GitHubCheckRun checkRunWith(
            final GitHubCheckRunStatus status, final GitHubCheckRunConclusion conclusion) {
        return GitHubCheckRunBuilder.builder(checkRun())
                .status(status)
                .conclusion(conclusion)
                .build();
    }

    public static GitHubCommitStatus commitStatus() {
        return GitHubCommitStatusBuilder.builder()
                .url(API + "/statuses/abc123")
                .id(6006L)
                .nodeId("SC_kwDOA")
                .state("success")
                .description("Build passed")
                .targetUrl("https://ci.example.com/build/1")
                .context("ci/build")
                .createdAt("2026-02-03T04:05:06Z")
                .updatedAt("2026-02-03T04:05:06Z")
                .creator(mock(GitHubSimpleUser.class))
                .build();
    }

    public static GitHubCombinedCommitStatus combinedStatus(final String state) {
        return GitHubCombinedCommitStatusBuilder.builder()
                .state(state)
                .statuses(List.of(mock(GitHubSimpleCommitStatus.class)))
                .sha(SHA)
                .totalCount(1)
                .repository(mock(GitHubMinimalRepository.class))
                .commitUrl(API + "/commits/abc123")
                .url(API + "/commits/abc123/status")
                .build();
    }
}
