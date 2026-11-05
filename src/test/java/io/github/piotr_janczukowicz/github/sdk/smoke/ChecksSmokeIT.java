package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubUpdateCheckRunStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitStatusState;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CombinedStatus;
import io.github.piotr_janczukowicz.github.sdk.checks.CommitStatus;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ChecksSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void status_shouldBeCreatedListedAndCombined_whenItIsReportedForACommit() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("status");
        final String context = ENV.name("ci");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-status.txt");
            final Commit commit =
                    repository.commit(SandboxBranches.branchSha(ENV, head)).orElseThrow();

            final CommitStatus pending = commit.createStatus(GitHubCommitStatusState.PENDING, context);
            final CommitStatus success = commit.createStatus(
                    GitHubCommitStatusState.SUCCESS, context, "Smoke passed", "https://example.com/smoke");
            final List<CommitStatus> statuses = commit.statuses().toList();
            final CombinedStatus combined = commit.combinedStatus().orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(pending.state()).isEqualTo("pending");
                softly.assertThat(pending.context()).isEqualTo(context);
                softly.assertThat(success.isSuccess()).isTrue();
                softly.assertThat(success.description()).hasValue("Smoke passed");
                softly.assertThat(success.targetUrl()).hasValue("https://example.com/smoke");
                softly.assertThat(statuses).extracting(CommitStatus::id).contains(pending.id(), success.id());
                softly.assertThat(combined.isSuccess()).isTrue();
                softly.assertThat(combined.statuses())
                        .anySatisfy(status -> assertThat(status.context()).isEqualTo(context));
            });
        } finally {
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void checkRuns_shouldBeListedForTheHeadCommit_whenTheyExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final Commit head = repository.branch(base).orElseThrow().commit();

        final List<CheckRun> checkRuns = listCheckRuns(head);

        assertThat(checkRuns).allSatisfy(checkRun -> {
            assertThat(checkRun.headSha()).isEqualTo(head.sha());
            assertThat(checkRun.name()).isNotBlank();
            assertThat(repository.checkRun(checkRun.id())).isPresent();
        });
    }

    private static List<CheckRun> listCheckRuns(final Commit commit) throws IOException {
        try {
            return commit.checkRuns().limit(5).toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("Fine-grained tokens have no Checks permission, so GitHub refuses reading check runs");
            throw e;
        }
    }

    @Test
    void checkRun_shouldBeEmpty_whenTheIdDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        assertThat(repository.checkRun(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void checkRun_shouldBeCreatedUpdatedCompletedAndRerequested_whenTheCredentialIsAGitHubApp() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String sha = repository.branch(base).orElseThrow().commit().sha();
        final CheckRun created = createCheckRun(repository, "smoke-" + ENV.runId(), sha);

        final CheckRun updated = created.update()
                .status(GitHubUpdateCheckRunStatus.IN_PROGRESS)
                .output("Smoke", "Running")
                .send();
        final CheckRun completed = updated.complete(GitHubUpdateCheckRunConclusion.SUCCESS);
        completed.rerequest();

        assertSoftly(softly -> {
            softly.assertThat(created.name()).isEqualTo("smoke-" + ENV.runId());
            softly.assertThat(updated.status()).isEqualTo(GitHubCheckRunStatus.IN_PROGRESS);
            softly.assertThat(completed.isSuccessful()).isTrue();
        });
    }

    private static CheckRun createCheckRun(final Repository repository, final String name, final String sha)
            throws IOException {
        try {
            return repository.createCheckRun(name, sha);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub lets only a GitHub App create check runs, so a token is refused");
            throw e;
        }
    }
}
