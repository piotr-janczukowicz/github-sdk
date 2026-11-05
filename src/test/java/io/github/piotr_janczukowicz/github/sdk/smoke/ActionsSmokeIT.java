package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.actions.Variable;
import io.github.piotr_janczukowicz.github.sdk.actions.Workflow;
import io.github.piotr_janczukowicz.github.sdk.actions.WorkflowJob;
import io.github.piotr_janczukowicz.github.sdk.actions.WorkflowRun;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ActionsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String WORKFLOW_VARIABLE = "SMOKE_GITHUB_WORKFLOW";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void variable_shouldBeCreatedListedUpdatedAndDeleted_whenItIsManaged() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = ENV.name("var").toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        final Variable created = createVariable(repository, name);
        try {
            final List<Variable> listed = repository.variables().toList();
            final Variable updated = created.update("second");
            final Optional<Variable> found = repository.variable(name);

            assertSoftly(softly -> {
                softly.assertThat(created.value()).isEqualTo("first");
                softly.assertThat(listed).extracting(Variable::name).contains(name);
                softly.assertThat(updated.value()).isEqualTo("second");
                softly.assertThat(found)
                        .hasValueSatisfying(
                                variable -> assertThat(variable.value()).isEqualTo("second"));
            });
        } finally {
            created.delete();
        }
        assertThat(repository.variable(name)).isEmpty();
    }

    private static Variable createVariable(final Repository repository, final String name) throws IOException {
        try {
            return repository.createVariable(name, "first");
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Variables permission, so GitHub refuses managing variables");
            throw e;
        }
    }

    @Test
    void workflowsAndRuns_shouldBeListedAndReloaded_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<Workflow> workflows = listWorkflows(repository);
        final List<WorkflowRun> runs = repository.workflowRuns().limit(3).toList();

        assertSoftly(softly -> {
            softly.assertThat(workflows).allSatisfy(workflow -> {
                assertThat(workflow.path()).startsWith(".github/workflows/");
                assertThat(repository.workflow(String.valueOf(workflow.id()))).isPresent();
            });
            softly.assertThat(runs).allSatisfy(run -> {
                assertThat(run.refresh().id()).isEqualTo(run.id());
                assertThat(run.jobs().toList())
                        .allSatisfy(job -> assertThat(job.runId()).isEqualTo(run.id()));
                assertThat(run.logs()).isNotNull();
            });
        });
    }

    private static List<Workflow> listWorkflows(final Repository repository) throws IOException {
        try {
            return repository.workflows().limit(5).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Actions permission, so GitHub refuses reading workflows");
            throw e;
        }
    }

    @Test
    void workflow_shouldBeDispatchedAndCancelled_whenTheSandboxHasADispatchableOne() throws IOException {
        final String file = System.getenv(WORKFLOW_VARIABLE);
        Assumptions.assumeTrue(
                file != null && !file.isBlank(),
                WORKFLOW_VARIABLE + " is not set to the file name of a workflow_dispatch workflow");
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String branch = repository.fetch().orElseThrow().response().defaultBranch();
        final Workflow workflow = repository.workflow(file).orElseThrow();

        final WorkflowRun started = workflow.dispatch(branch);
        final WorkflowRun cancelled = started.cancel();
        final List<WorkflowJob> jobs = cancelled.jobs().toList();
        final List<WorkflowRun> recent = workflow.runs().limit(5).toList();

        assertSoftly(softly -> {
            softly.assertThat(started.workflowId()).isEqualTo(workflow.id());
            softly.assertThat(started.event()).isEqualTo("workflow_dispatch");
            softly.assertThat(cancelled.id()).isEqualTo(started.id());
            softly.assertThat(recent).extracting(WorkflowRun::id).contains(started.id());
            softly.assertThat(jobs).allSatisfy(job -> assertThat(job.runId()).isEqualTo(started.id()));
        });
    }

    @Test
    void workflowRun_shouldBeEmpty_whenTheIdDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        assertThat(repository.workflowRun(Long.MAX_VALUE)).isEmpty();
    }
}
