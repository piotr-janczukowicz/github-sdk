package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowJobsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowRunsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJob;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobBuilder;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobConclusion;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobStatus;
import io.github.piotr_janczukowicz.github.api.actions.GitHubJobStep;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class WorkflowJobTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ActionsFixtures.JOB_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubJob response = ActionsFixtures.job();
    private final WorkflowJob sut = new WorkflowJob(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenJobIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.runId()).isEqualTo(ActionsFixtures.RUN_ID);
            softly.assertThat(sut.name()).isEqualTo("test");
            softly.assertThat(sut.status()).isEqualTo(GitHubJobStatus.COMPLETED);
            softly.assertThat(sut.conclusion()).hasValue(GitHubJobConclusion.SUCCESS);
            softly.assertThat(sut.steps()).isEmpty();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void steps_shouldReturnTheSteps_whenGitHubListsThem() {
        final GitHubJobStep step = mock(GitHubJobStep.class);
        final GitHubJob withSteps =
                GitHubJobBuilder.builder(response).steps(List.of(step)).build();

        final List<GitHubJobStep> steps = new WorkflowJob(repository, withSteps).steps();

        assertThat(steps).containsExactly(step);
    }

    @Test
    void optionalAccessors_shouldBeEmptyOrNeutral_whenGitHubReportsNothing() {
        final GitHubJob bare =
                GitHubJobBuilder.builder(response).conclusion(null).steps(null).build();
        final WorkflowJob job = new WorkflowJob(repository, bare);

        assertSoftly(softly -> {
            softly.assertThat(job.conclusion()).isEmpty();
            softly.assertThat(job.steps()).isEmpty();
        });
    }

    @Test
    void outcomeChecks_shouldMatchStatusAndConclusion_whenCalled() {
        final WorkflowJob failed = job(GitHubJobStatus.COMPLETED, GitHubJobConclusion.FAILURE);
        final WorkflowJob running = job(GitHubJobStatus.IN_PROGRESS, null);

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
    void refresh_shouldReturnTheReloadedJob_whenItExists() throws IOException {
        final GitHubJob reloaded = ActionsFixtures.jobWith(GitHubJobStatus.QUEUED, null);
        final GitHubActionsWorkflowJobsApi jobs = github.actions().workflowJobs();
        doReturn(Optional.of(reloaded)).when(jobs).getJobForWorkflowRun(OWNER, NAME, ID);

        final WorkflowJob result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheJobIsGone() throws IOException {
        final GitHubActionsWorkflowJobsApi jobs = github.actions().workflowJobs();
        doReturn(Optional.empty()).when(jobs).getJobForWorkflowRun(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void rerun_shouldRerunAndReturnTheJob_whenCalled() throws IOException {
        final GitHubJob reloaded = ActionsFixtures.jobWith(GitHubJobStatus.QUEUED, null);
        final GitHubActionsWorkflowJobsApi jobs = github.actions().workflowJobs();
        doReturn(Optional.of(reloaded)).when(jobs).getJobForWorkflowRun(OWNER, NAME, ID);
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();

        final WorkflowJob result = sut.rerun();

        assertThat(result.status()).isEqualTo(GitHubJobStatus.QUEUED);
        verify(runs).reRunJobForWorkflowRun(OWNER, NAME, ID);
    }

    @Test
    void logs_shouldReturnTheRedirectLink_whenCalled() throws IOException {
        final URI link = URI.create("https://logs.example.com/job/9009.txt");
        final GitHubActionsWorkflowJobsApi jobs = github.actions().workflowJobs();
        doReturn(link).when(jobs).downloadJobLogsForWorkflowRun(OWNER, NAME, ID);

        final URI result = sut.logs();

        assertThat(result).isEqualTo(link);
    }

    private WorkflowJob job(final GitHubJobStatus status, final GitHubJobConclusion conclusion) {
        return new WorkflowJob(repository, ActionsFixtures.jobWith(status, conclusion));
    }
}
