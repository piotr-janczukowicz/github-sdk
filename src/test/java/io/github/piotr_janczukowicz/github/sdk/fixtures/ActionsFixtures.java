package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifact;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifactBuilder;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJob;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobBuilder;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobConclusion;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobStatus;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflow;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowBuilder;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRun;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRunBuilder;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowState;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariable;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariableBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import java.time.Instant;

public final class ActionsFixtures {

    public static final long WORKFLOW_ID = 7007L;
    public static final long RUN_ID = 8008L;
    public static final long JOB_ID = 9009L;
    public static final long ARTIFACT_ID = 1010L;
    public static final String SHA = "abc123";

    private static final String API = "https://api.github.com/repos/octocat/hello-world";
    private static final Instant MOMENT = Instant.parse("2026-11-05T10:00:00Z");

    private ActionsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubWorkflow workflow() {
        return GitHubWorkflowBuilder.builder()
                .id(WORKFLOW_ID)
                .nodeId("W_kwDOA")
                .name("build")
                .path(".github/workflows/build.yml")
                .state(GitHubWorkflowState.ACTIVE)
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .url(API + "/actions/workflows/7007")
                .htmlUrl("https://github.com/octocat/hello-world/blob/main/.github/workflows/build.yml")
                .badgeUrl("https://github.com/octocat/hello-world/workflows/build/badge.svg")
                .build();
    }

    public static GitHubWorkflow workflowWith(final GitHubWorkflowState state) {
        return GitHubWorkflowBuilder.builder(workflow()).state(state).build();
    }

    public static GitHubWorkflowRun workflowRun() {
        return GitHubWorkflowRunBuilder.builder()
                .id(RUN_ID)
                .name("build")
                .nodeId("WFR_kwDOA")
                .headBranch("main")
                .headSha(SHA)
                .path(".github/workflows/build.yml")
                .runNumber(12)
                .runAttempt(1)
                .event("push")
                .status("completed")
                .conclusion("success")
                .workflowId(WORKFLOW_ID)
                .url(API + "/actions/runs/8008")
                .htmlUrl("https://github.com/octocat/hello-world/actions/runs/8008")
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .jobsUrl(API + "/actions/runs/8008/jobs")
                .logsUrl(API + "/actions/runs/8008/logs")
                .checkSuiteUrl(API + "/check-suites/1")
                .artifactsUrl(API + "/actions/runs/8008/artifacts")
                .cancelUrl(API + "/actions/runs/8008/cancel")
                .rerunUrl(API + "/actions/runs/8008/rerun")
                .workflowUrl(API + "/actions/workflows/7007")
                .displayTitle("Build")
                .repository(mock(GitHubMinimalRepository.class))
                .headRepository(mock(GitHubMinimalRepository.class))
                .build();
    }

    public static GitHubWorkflowRun workflowRunWith(final String status, final String conclusion) {
        return GitHubWorkflowRunBuilder.builder(workflowRun())
                .status(status)
                .conclusion(conclusion)
                .build();
    }

    public static GitHubJob job() {
        return GitHubJobBuilder.builder()
                .id(JOB_ID)
                .runId(RUN_ID)
                .runUrl(API + "/actions/runs/8008")
                .runAttempt(1)
                .nodeId("CR_kwDOA")
                .headSha(SHA)
                .url(API + "/actions/jobs/9009")
                .htmlUrl("https://github.com/octocat/hello-world/actions/runs/8008/job/9009")
                .status(GitHubJobStatus.COMPLETED)
                .conclusion(GitHubJobConclusion.SUCCESS)
                .createdAt(MOMENT)
                .startedAt(MOMENT)
                .completedAt(MOMENT)
                .name("test")
                .steps(java.util.List.of())
                .checkRunUrl(API + "/check-runs/9009")
                .labels(java.util.List.of("ubuntu-latest"))
                .build();
    }

    public static GitHubJob jobWith(final GitHubJobStatus status, final GitHubJobConclusion conclusion) {
        return GitHubJobBuilder.builder(job())
                .status(status)
                .conclusion(conclusion)
                .build();
    }

    public static GitHubArtifact artifact() {
        return GitHubArtifactBuilder.builder()
                .id(ARTIFACT_ID)
                .nodeId("MDg6QXJ0aWZhY3Q")
                .name("report")
                .sizeInBytes(2048)
                .url(API + "/actions/artifacts/1010")
                .archiveDownloadUrl(API + "/actions/artifacts/1010/zip")
                .expired(false)
                .createdAt(MOMENT)
                .expiresAt(MOMENT.plusSeconds(86_400))
                .updatedAt(MOMENT)
                .build();
    }

    public static GitHubActionsVariable variable() {
        return GitHubActionsVariableBuilder.builder()
                .name("DEPLOY_ENV")
                .value("staging")
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .build();
    }
}
