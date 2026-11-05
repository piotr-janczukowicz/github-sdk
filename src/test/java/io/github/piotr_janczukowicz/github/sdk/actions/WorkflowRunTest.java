package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowRunsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifact;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJob;
import io.github.piotr_janczukowicz.github.api.actions.GitHubReRunWorkflowFailedJobsPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubReRunWorkflowPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRun;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRunBuilder;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class WorkflowRunTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ActionsFixtures.RUN_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubWorkflowRun response = ActionsFixtures.workflowRun();
    private final WorkflowRun sut = new WorkflowRun(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenRunIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.workflowId()).isEqualTo(ActionsFixtures.WORKFLOW_ID);
            softly.assertThat(sut.name()).hasValue("build");
            softly.assertThat(sut.headBranch()).hasValue("main");
            softly.assertThat(sut.headSha()).isEqualTo(ActionsFixtures.SHA);
            softly.assertThat(sut.event()).isEqualTo("push");
            softly.assertThat(sut.status()).hasValue("completed");
            softly.assertThat(sut.conclusion()).hasValue("success");
            softly.assertThat(sut.runNumber()).isEqualTo(12);
            softly.assertThat(sut.runAttempt()).hasValue(1);
            softly.assertThat(sut.htmlUrl()).endsWith("/actions/runs/8008");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void optionalAccessors_shouldBeEmpty_whenGitHubReportsNothing() {
        final GitHubWorkflowRun bare = GitHubWorkflowRunBuilder.builder(response)
                .name(null)
                .headBranch(null)
                .status(null)
                .conclusion(null)
                .runAttempt(null)
                .build();
        final WorkflowRun run = new WorkflowRun(repository, bare);

        assertSoftly(softly -> {
            softly.assertThat(run.name()).isEmpty();
            softly.assertThat(run.headBranch()).isEmpty();
            softly.assertThat(run.status()).isEmpty();
            softly.assertThat(run.conclusion()).isEmpty();
            softly.assertThat(run.runAttempt()).isEmpty();
        });
    }

    @Test
    void outcomeChecks_shouldMatchStatusAndConclusion_whenCalled() {
        final WorkflowRun failed = run("completed", "failure");
        final WorkflowRun running = run("in_progress", null);

        assertSoftly(softly -> {
            softly.assertThat(sut.isCompleted()).isTrue();
            softly.assertThat(sut.isSuccessful()).isTrue();
            softly.assertThat(failed.isCompleted()).isTrue();
            softly.assertThat(failed.isSuccessful()).isFalse();
            softly.assertThat(running.isCompleted()).isFalse();
            softly.assertThat(running.isSuccessful()).isFalse();
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedRun_whenItExists() throws IOException {
        final GitHubWorkflowRun reloaded = ActionsFixtures.workflowRunWith("queued", null);
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(reloaded)).when(runs).getWorkflowRun(OWNER, NAME, ID);

        final WorkflowRun result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRunIsGone() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.empty()).when(runs).getWorkflowRun(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void attempt_shouldReturnTheAttempt_whenGitHubHasIt() throws IOException {
        final GitHubWorkflowRun earlier = ActionsFixtures.workflowRun();
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(earlier)).when(runs).getWorkflowRunAttempt(OWNER, NAME, ID, 1L);

        final Optional<WorkflowRun> found = sut.attempt(1L);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(earlier);
    }

    @Test
    void attempt_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.empty()).when(runs).getWorkflowRunAttempt(OWNER, NAME, ID, 9L);

        final Optional<WorkflowRun> found = sut.attempt(9L);

        assertThat(found).isEmpty();
    }

    @Test
    void cancel_shouldCancelAndReturnTheRun_whenCalled() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("completed", "cancelled"));

        final WorkflowRun result = sut.cancel();

        assertThat(result.conclusion()).hasValue("cancelled");
        verify(runs).cancelWorkflowRun(OWNER, NAME, ID);
    }

    @Test
    void forceCancel_shouldForceCancelAndReturnTheRun_whenCalled() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("completed", "cancelled"));

        final WorkflowRun result = sut.forceCancel();

        assertThat(result.conclusion()).hasValue("cancelled");
        verify(runs).forceCancelWorkflowRun(OWNER, NAME, ID);
    }

    @Test
    void rerun_shouldRunWithoutDebugLogging_whenNoFlagIsGiven() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("queued", null));

        final WorkflowRun result = sut.rerun();

        assertThat(result.status()).hasValue("queued");
        verify(runs).reRunWorkflow(OWNER, NAME, ID, new GitHubReRunWorkflowPayload(false));
    }

    @Test
    void rerun_shouldEnableDebugLogging_whenAskedTo() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("queued", null));

        final WorkflowRun result = sut.rerun(true);

        assertThat(result.status()).hasValue("queued");
        verify(runs).reRunWorkflow(OWNER, NAME, ID, new GitHubReRunWorkflowPayload(true));
    }

    @Test
    void rerunFailedJobs_shouldRunWithoutDebugLogging_whenNoFlagIsGiven() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("queued", null));

        final WorkflowRun result = sut.rerunFailedJobs();

        assertThat(result.status()).hasValue("queued");
        verify(runs).reRunWorkflowFailedJobs(OWNER, NAME, ID, new GitHubReRunWorkflowFailedJobsPayload(false));
    }

    @Test
    void rerunFailedJobs_shouldEnableDebugLogging_whenAskedTo() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = reloadable(ActionsFixtures.workflowRunWith("queued", null));

        final WorkflowRun result = sut.rerunFailedJobs(true);

        assertThat(result.status()).hasValue("queued");
        verify(runs).reRunWorkflowFailedJobs(OWNER, NAME, ID, new GitHubReRunWorkflowFailedJobsPayload(true));
    }

    @Test
    void delete_shouldDeleteTheRun_whenCalled() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();

        sut.delete();

        verify(runs).deleteWorkflowRun(OWNER, NAME, ID);
    }

    @Test
    void jobs_shouldStreamTheJobsOfTheLatestAttempt_whenTheyExist() throws IOException {
        final GitHubJob listed = ActionsFixtures.job();
        when(github.actions().workflowJobs().listJobsForWorkflowRun(eq(OWNER), eq(NAME), eq(ID), any(), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowJob> jobs = sut.jobs().toList();

        assertThat(jobs).singleElement().satisfies(job -> {
            assertThat(job.response()).isSameAs(listed);
            assertThat(job.repository()).isSameAs(repository);
        });
    }

    @Test
    void jobs_shouldStreamTheJobsOfThatAttempt_whenAnAttemptIsGiven() throws IOException {
        final GitHubJob listed = ActionsFixtures.job();
        when(github
                        .actions()
                        .workflowJobs()
                        .listJobsForWorkflowRunAttempt(eq(OWNER), eq(NAME), eq(ID), eq(2L), any())
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowJob> jobs = sut.jobs(2L).toList();

        assertThat(jobs)
                .singleElement()
                .satisfies(job -> assertThat(job.response()).isSameAs(listed));
    }

    @Test
    void artifacts_shouldStreamTheArtifacts_whenTheyExist() throws IOException {
        final GitHubArtifact listed = ActionsFixtures.artifact();
        when(github.actions().artifacts().listWorkflowRunArtifacts(eq(OWNER), eq(NAME), eq(ID), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<Artifact> artifacts = sut.artifacts().toList();

        assertThat(artifacts).singleElement().satisfies(artifact -> {
            assertThat(artifact.response()).isSameAs(listed);
            assertThat(artifact.repository()).isSameAs(repository);
        });
    }

    @Test
    void logs_shouldReturnTheRedirectLink_whenCalled() throws IOException {
        final URI link = URI.create("https://logs.example.com/run/8008.zip");
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(link).when(runs).downloadWorkflowRunLogs(OWNER, NAME, ID);

        final URI result = sut.logs();

        assertThat(result).isEqualTo(link);
    }

    @Test
    void deleteLogs_shouldDeleteTheLogs_whenCalled() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();

        sut.deleteLogs();

        verify(runs).deleteWorkflowRunLogs(OWNER, NAME, ID);
    }

    private WorkflowRun run(final String status, final String conclusion) {
        return new WorkflowRun(repository, ActionsFixtures.workflowRunWith(status, conclusion));
    }

    private GitHubActionsWorkflowRunsApi reloadable(final GitHubWorkflowRun reloaded) throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(reloaded)).when(runs).getWorkflowRun(OWNER, NAME, ID);
        return runs;
    }

    @Test
    void attemptNumbers_shouldThrowIllegalArgumentException_whenTheAttemptIsBelowOne() {
        final Throwable attempt = catchThrowable(() -> sut.attempt(0));
        final Throwable jobs = catchThrowable(() -> sut.jobs(-1));

        assertSoftly(softly -> {
            softly.assertThat(attempt)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("attempt");
            softly.assertThat(jobs).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("attempt");
        });
    }
}
