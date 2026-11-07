package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsArtifactsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsSecretsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsVariablesApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowRunsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsWorkflowsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubArtifact;
import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateRepoVariablePayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListWorkflowRunsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflow;
import io.github.piotr_janczukowicz.github.api.actions.GitHubWorkflowRun;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsSecret;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariable;
import io.github.piotr_janczukowicz.github.sdk.actions.Artifact;
import io.github.piotr_janczukowicz.github.sdk.actions.Variable;
import io.github.piotr_janczukowicz.github.sdk.actions.Workflow;
import io.github.piotr_janczukowicz.github.sdk.actions.WorkflowRun;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ActionsFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryActionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String VARIABLE = "DEPLOY_ENV";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void workflows_shouldStreamTheWorkflows_whenTheyExist() throws IOException {
        final GitHubWorkflow listed = ActionsFixtures.workflow();
        when(github.actions().workflows().listRepoWorkflows(eq(OWNER), eq(NAME), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<Workflow> workflows = sut.workflows().toList();

        assertThat(workflows)
                .singleElement()
                .satisfies(workflow -> assertSoftly(softly -> {
                    softly.assertThat(workflow.response()).isSameAs(listed);
                    softly.assertThat(workflow.repository()).isSameAs(sut);
                }));
    }

    @Test
    void workflow_shouldReturnTheWorkflow_whenGitHubHasIt() throws IOException {
        final GitHubWorkflow loaded = ActionsFixtures.workflow();
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.of(loaded)).when(workflows).getWorkflow(OWNER, NAME, "build.yml");

        final Optional<Workflow> found = sut.workflow("build.yml");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    @Test
    void workflow_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsWorkflowsApi workflows = github.actions().workflows();
        doReturn(Optional.empty()).when(workflows).getWorkflow(OWNER, NAME, "missing.yml");

        final Optional<Workflow> found = sut.workflow("missing.yml");

        assertThat(found).isEmpty();
    }

    @Test
    void workflow_shouldThrowIllegalArgumentException_whenTheKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.workflow(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("idOrFileName");
    }

    @Test
    void workflowRuns_shouldStreamTheRuns_whenTheyExist() throws IOException {
        final GitHubWorkflowRun listed = ActionsFixtures.workflowRun();
        when(github.actions().workflowRuns().listWorkflowRunsForRepo(eq(OWNER), eq(NAME), any(), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowRun> runs = sut.workflowRuns().toList();

        assertThat(runs)
                .singleElement()
                .satisfies(run -> assertSoftly(softly -> {
                    softly.assertThat(run.response()).isSameAs(listed);
                    softly.assertThat(run.repository()).isSameAs(sut);
                }));
    }

    @Test
    void workflowRuns_shouldPassTheFilterOn_whenOneIsGiven() throws IOException {
        final GitHubListWorkflowRunsForRepoFilter filter = mock(GitHubListWorkflowRunsForRepoFilter.class);
        final GitHubWorkflowRun listed = ActionsFixtures.workflowRun();
        when(github.actions().workflowRuns().listWorkflowRunsForRepo(eq(OWNER), eq(NAME), eq(filter), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<WorkflowRun> runs = sut.workflowRuns(filter).toList();

        assertThat(runs).singleElement().extracting(WorkflowRun::response).isSameAs(listed);
    }

    @Test
    void workflowRuns_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.workflowRuns(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void workflowRun_shouldReturnTheRun_whenGitHubHasIt() throws IOException {
        final GitHubWorkflowRun loaded = ActionsFixtures.workflowRun();
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.of(loaded)).when(runs).getWorkflowRun(OWNER, NAME, ActionsFixtures.RUN_ID);

        final Optional<WorkflowRun> found = sut.workflowRun(ActionsFixtures.RUN_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    @Test
    void workflowRun_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsWorkflowRunsApi runs = github.actions().workflowRuns();
        doReturn(Optional.empty()).when(runs).getWorkflowRun(OWNER, NAME, 1L);

        final Optional<WorkflowRun> found = sut.workflowRun(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void artifacts_shouldStreamTheArtifacts_whenTheyExist() throws IOException {
        when(github.actions().artifacts().listArtifactsForRepo(eq(OWNER), eq(NAME), any()).stream())
                .thenReturn(Stream.of(ActionsFixtures.artifact()));

        final List<Artifact> artifacts = sut.artifacts().toList();

        assertThat(artifacts)
                .singleElement()
                .satisfies(artifact -> assertSoftly(softly -> {
                    softly.assertThat(artifact.id()).isEqualTo(ActionsFixtures.ARTIFACT_ID);
                    softly.assertThat(artifact.repository()).isSameAs(sut);
                }));
    }

    @Test
    void artifact_shouldReturnTheArtifact_whenGitHubHasIt() throws IOException {
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();
        final GitHubArtifact loaded = ActionsFixtures.artifact();
        doReturn(Optional.of(loaded)).when(artifacts).getArtifact(OWNER, NAME, ActionsFixtures.ARTIFACT_ID);

        final Optional<Artifact> found = sut.artifact(ActionsFixtures.ARTIFACT_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void artifact_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsArtifactsApi artifacts = github.actions().artifacts();
        doReturn(Optional.empty()).when(artifacts).getArtifact(OWNER, NAME, 1L);

        final Optional<Artifact> found = sut.artifact(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void variables_shouldStreamTheVariables_whenTheyExist() throws IOException {
        when(github.actions().variables().listRepoVariables(eq(OWNER), eq(NAME), any()).stream())
                .thenReturn(Stream.of(ActionsFixtures.variable()));

        final List<Variable> variables = sut.variables().toList();

        assertThat(variables)
                .singleElement()
                .satisfies(variable -> assertSoftly(softly -> {
                    softly.assertThat(variable.name()).isEqualTo(VARIABLE);
                    softly.assertThat(variable.repository()).isSameAs(sut);
                }));
    }

    @Test
    void variable_shouldReturnTheVariable_whenGitHubHasIt() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();
        final GitHubActionsVariable loaded = ActionsFixtures.variable();
        doReturn(Optional.of(loaded)).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Optional<Variable> found = sut.variable(VARIABLE);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void variable_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();
        doReturn(Optional.empty()).when(variables).getRepoVariable(OWNER, NAME, "MISSING");

        final Optional<Variable> found = sut.variable("MISSING");

        assertThat(found).isEmpty();
    }

    @Test
    void variable_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.variable(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("variableName");
    }

    @Test
    void variable_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.variable(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("variableName");
    }

    @Test
    void createVariable_shouldCreateAndReturnTheVariable_whenCalled() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();
        final GitHubActionsVariable created = ActionsFixtures.variable();
        doReturn(Optional.of(created)).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Variable result = sut.createVariable(VARIABLE, "staging");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.repository()).isSameAs(sut);
        });
        verify(variables).createRepoVariable(OWNER, NAME, new GitHubCreateRepoVariablePayload(VARIABLE, "staging"));
    }

    @Test
    void createVariable_shouldThrowIllegalStateException_whenTheVariableIsGone() throws IOException {
        final GitHubActionsVariablesApi variables = github.actions().variables();
        doReturn(Optional.empty()).when(variables).getRepoVariable(OWNER, NAME, VARIABLE);

        final Throwable thrown = catchThrowable(() -> sut.createVariable(VARIABLE, "staging"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void createVariable_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createVariable(" ", "staging"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("variableName");
    }

    @Test
    void createVariable_shouldThrowNullPointerException_whenValueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createVariable(VARIABLE, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void secrets_shouldStreamTheSecrets_whenTheyExist() throws IOException {
        final GitHubActionsSecret secret = mock(GitHubActionsSecret.class);
        when(github.actions().secrets().listRepoSecrets(eq(OWNER), eq(NAME), any()).stream())
                .thenReturn(Stream.of(secret));

        final List<GitHubActionsSecret> secrets = sut.secrets().toList();

        assertThat(secrets).containsExactly(secret);
    }

    @Test
    void deleteSecret_shouldDeleteTheSecret_whenCalled() throws IOException {
        final GitHubActionsSecretsApi secrets = github.actions().secrets();

        sut.deleteSecret("TOKEN");

        verify(secrets).deleteRepoSecret(OWNER, NAME, "TOKEN");
    }

    @Test
    void deleteSecret_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.deleteSecret(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("secretName");
    }
}
