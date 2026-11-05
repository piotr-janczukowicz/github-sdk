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
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateWorkflowDispatchPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListWorkflowRunsFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflow;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowDispatchResponse;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRun;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRunId;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowState;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class WorkflowTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ActionsFixtures.WORKFLOW_ID;
    private static final String KEY = "7007";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubWorkflow response = ActionsFixtures.workflow();
    private final Workflow sut = new Workflow(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenWorkflowIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("build");
            softly.assertThat(sut.path()).isEqualTo(".github/workflows/build.yml");
            softly.assertThat(sut.state()).isEqualTo(GitHubWorkflowState.ACTIVE);
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new Workflow(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Workflow(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void isActive_shouldBeFalse_whenTheWorkflowIsDisabled() {
        final Workflow disabled =
                new Workflow(repository, ActionsFixtures.workflowWith(GitHubWorkflowState.DISABLED_MANUALLY));

        assertThat(disabled.isActive()).isFalse();
    }

    @Test
    void refresh_shouldReturnTheReloadedWorkflow_whenItExists() throws IOException {
        final GitHubWorkflow reloaded = ActionsFixtures.workflowWith(GitHubWorkflowState.DISABLED_MANUALLY);
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.of(reloaded)).when(workflows).getWorkflow(OWNER, NAME, KEY);

        final Workflow result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheWorkflowIsGone() throws IOException {
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.empty()).when(workflows).getWorkflow(OWNER, NAME, KEY);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void enable_shouldEnableAndReturnTheWorkflow_whenCalled() throws IOException {
        final GitHubWorkflow reloaded = ActionsFixtures.workflow();
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.of(reloaded)).when(workflows).getWorkflow(OWNER, NAME, KEY);

        final Workflow result = sut.enable();

        assertThat(result.response()).isSameAs(reloaded);
        verify(workflows).enableWorkflow(OWNER, NAME, KEY);
    }

    @Test
    void disable_shouldDisableAndReturnTheWorkflow_whenCalled() throws IOException {
        final GitHubWorkflow reloaded = ActionsFixtures.workflowWith(GitHubWorkflowState.DISABLED_MANUALLY);
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.of(reloaded)).when(workflows).getWorkflow(OWNER, NAME, KEY);

        final Workflow result = sut.disable();

        assertThat(result.response()).isSameAs(reloaded);
        verify(workflows).disableWorkflow(OWNER, NAME, KEY);
    }

    @Test
    void dispatch_shouldSendRefAndInputsAndReturnTheRun_whenInputsAreGiven() throws IOException {
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        final GitHubWorkflowDispatchResponse dispatched = mock(GitHubWorkflowDispatchResponse.class);
        when(dispatched.workflowRunId()).thenReturn(new GitHubWorkflowRunId(ActionsFixtures.RUN_ID));
        doReturn(dispatched).when(workflows).createWorkflowDispatch(eq(OWNER), eq(NAME), eq(KEY), any());
        final GitHubWorkflowRun started = ActionsFixtures.workflowRun();
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(started)).when(runs).getWorkflowRun(OWNER, NAME, ActionsFixtures.RUN_ID);
        final ArgumentCaptor<GitHubCreateWorkflowDispatchPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateWorkflowDispatchPayload.class);

        final WorkflowRun run = sut.dispatch("main", Map.of("environment", "staging"));

        verify(workflows).createWorkflowDispatch(eq(OWNER), eq(NAME), eq(KEY), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(run.response()).isSameAs(started);
            softly.assertThat(payload.getValue().ref()).isEqualTo("main");
            softly.assertThat(payload.getValue().inputs()).containsEntry("environment", "staging");
        });
    }

    @Test
    void dispatch_shouldSendNoInputs_whenOnlyTheRefIsGiven() throws IOException {
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        final GitHubWorkflowDispatchResponse dispatched = mock(GitHubWorkflowDispatchResponse.class);
        when(dispatched.workflowRunId()).thenReturn(new GitHubWorkflowRunId(ActionsFixtures.RUN_ID));
        doReturn(dispatched).when(workflows).createWorkflowDispatch(eq(OWNER), eq(NAME), eq(KEY), any());
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(ActionsFixtures.workflowRun()))
                .when(runs)
                .getWorkflowRun(OWNER, NAME, ActionsFixtures.RUN_ID);
        final ArgumentCaptor<GitHubCreateWorkflowDispatchPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateWorkflowDispatchPayload.class);

        final WorkflowRun run = sut.dispatch("main");

        verify(workflows).createWorkflowDispatch(eq(OWNER), eq(NAME), eq(KEY), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(run.id()).isEqualTo(ActionsFixtures.RUN_ID);
            softly.assertThat(payload.getValue().inputs()).isEmpty();
        });
    }

    @Test
    void dispatch_shouldThrowIllegalStateException_whenTheStartedRunIsGone() throws IOException {
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        final GitHubWorkflowDispatchResponse dispatched = mock(GitHubWorkflowDispatchResponse.class);
        when(dispatched.workflowRunId()).thenReturn(new GitHubWorkflowRunId(ActionsFixtures.RUN_ID));
        doReturn(dispatched).when(workflows).createWorkflowDispatch(eq(OWNER), eq(NAME), eq(KEY), any());
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.empty()).when(runs).getWorkflowRun(OWNER, NAME, ActionsFixtures.RUN_ID);

        final Throwable thrown = catchThrowable(() -> sut.dispatch("main"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void dispatch_shouldThrowIllegalArgumentException_whenRefIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.dispatch(" ", Map.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ref");
    }

    @Test
    void dispatch_shouldThrowNullPointerException_whenRefIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dispatch(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("ref");
    }

    @Test
    void dispatch_shouldThrowNullPointerException_whenInputsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.dispatch("main", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("inputs");
    }

    @Test
    void runs_shouldStreamTheRuns_whenTheyExist() throws IOException {
        final GitHubWorkflowRun listed = ActionsFixtures.workflowRun();
        when(github
                        .actions()
                        .workflowRuns()
                        .listWorkflowRuns(eq(OWNER), eq(NAME), eq(KEY), eq(GitHubListWorkflowRunsFilter.empty()), any())
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowRun> runs = sut.runs().toList();

        assertThat(runs)
                .singleElement()
                .satisfies(run -> assertSoftly(softly -> {
                    softly.assertThat(run.response()).isSameAs(listed);
                    softly.assertThat(run.repository()).isSameAs(repository);
                }));
    }

    @Test
    void runs_shouldPassTheFilterOn_whenOneIsGiven() throws IOException {
        final GitHubListWorkflowRunsFilter filter = mock(GitHubListWorkflowRunsFilter.class);
        final GitHubWorkflowRun listed = ActionsFixtures.workflowRun();
        when(github.actions().workflowRuns().listWorkflowRuns(eq(OWNER), eq(NAME), eq(KEY), eq(filter), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowRun> runs = sut.runs(filter).toList();

        assertThat(runs)
                .singleElement()
                .satisfies(run -> assertThat(run.response()).isSameAs(listed));
    }

    @Test
    void runs_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.runs(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void dispatch_shouldThrowNullPointerException_whenAnInputValueIsNull() {
        final Map<String, Object> inputs = new HashMap<>();
        inputs.put("level", null);

        final Throwable thrown = catchThrowable(() -> sut.dispatch("main", inputs));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("inputs[level]");
    }
}
