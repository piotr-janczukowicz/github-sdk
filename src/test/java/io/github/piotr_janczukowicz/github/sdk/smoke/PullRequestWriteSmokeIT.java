package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestBranchUpdateResult;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestMergeMethod;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestState;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PullRequestWriteSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void pullRequest_shouldBeUpdatedClosedReopenedAndCommented_whenItIsOpen() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("write");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-write.txt");
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("write")));

            final PullRequest updated =
                    pullRequest.update().title(ENV.name("renamed")).send();
            final PullRequest closed = updated.close();
            final PullRequest reopened = closed.reopen();
            final Issue issue = reopened.issue();
            final String comment = issue.comment("smoke comment").body().orElseThrow();
            pullRequest = reopened;

            assertSoftly(softly -> {
                softly.assertThat(updated.title()).isEqualTo(ENV.name("renamed"));
                softly.assertThat(closed.state()).isEqualTo(PullRequestState.CLOSED);
                softly.assertThat(reopened.state()).isEqualTo(PullRequestState.OPEN);
                softly.assertThat(comment).isEqualTo("smoke comment");
            });
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void pullRequest_shouldBeMerged_whenItTargetsAThrowawayBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String main = repository.fetch().orElseThrow().response().defaultBranch();
        final String base = ENV.name("merge-base");
        final String head = ENV.name("merge-head");
        final String baseSha = SandboxBranches.branchSha(ENV, main);
        SandboxBranches.createBranch(ENV, base, baseSha);
        SandboxBranches.createBranch(ENV, head, baseSha);
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-merge.txt");
            final PullRequest created =
                    repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("merge")));

            final PullRequest merged = created.merge(GitHubPullRequestMergeMethod.SQUASH);

            assertSoftly(softly -> {
                softly.assertThat(created.isMerged()).isFalse();
                softly.assertThat(merged.isMerged()).isTrue();
                softly.assertThat(merged.state()).isEqualTo(PullRequestState.CLOSED);
            });
        } finally {
            SandboxBranches.deleteBranch(ENV, head);
            SandboxBranches.deleteBranch(ENV, base);
        }
    }

    @Test
    void pullRequest_shouldGiveDiffPatchAndBranchUpdateMessage_whenTheBaseMovedOn() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String main = repository.fetch().orElseThrow().response().defaultBranch();
        final String base = ENV.name("diff-base");
        final String head = ENV.name("diff-head");
        final String baseSha = SandboxBranches.branchSha(ENV, main);
        SandboxBranches.createBranch(ENV, base, baseSha);
        SandboxBranches.createBranch(ENV, head, baseSha);
        PullRequest pullRequest = null;
        try {
            final String file = "smoke/" + ENV.runId() + "-diff.txt";
            SandboxBranches.commitFile(ENV, head, file);
            SandboxBranches.commitFile(ENV, base, "smoke/" + ENV.runId() + "-base.txt");
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("diff")));

            final String diff = pullRequest.diff().orElseThrow();
            final String patch = pullRequest.patch().orElseThrow();
            final GitHubPullRequestBranchUpdateResult update = pullRequest.updateBranch();

            assertSoftly(softly -> {
                softly.assertThat(diff).contains(file);
                softly.assertThat(patch).contains(file);
                softly.assertThat(update.message()).isNotBlank();
            });
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
            SandboxBranches.deleteBranch(ENV, base);
        }
    }
}
