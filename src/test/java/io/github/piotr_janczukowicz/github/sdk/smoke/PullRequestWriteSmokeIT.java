package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayloadBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import io.github.piotr_janczukowicz.github.sdk.Issue;
import io.github.piotr_janczukowicz.github.sdk.MergeMethod;
import io.github.piotr_janczukowicz.github.sdk.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.PullRequestState;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        final GitHub github = ENV.github();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().record().defaultBranch();
        final String head = ENV.name("write");
        final String baseSha = branchSha(github, base);
        createBranch(github, head, baseSha);
        PullRequest pullRequest = null;
        try {
            commitFile(github, head, "smoke/" + ENV.runId() + "-write.txt");
            pullRequest = repository.createPullRequest(newPullRequest(head, base, ENV.name("write")));

            final PullRequest updated =
                    pullRequest.update().title(ENV.name("renamed")).send();
            final PullRequest closed = updated.close();
            final PullRequest reopened = closed.reopen();
            final Issue issue = reopened.issue();
            final String comment = issue.comment("smoke comment").body().orElseThrow();
            pullRequest = reopened;

            assertThat(updated.title()).isEqualTo(ENV.name("renamed"));
            assertThat(closed.state()).isEqualTo(PullRequestState.CLOSED);
            assertThat(reopened.state()).isEqualTo(PullRequestState.OPEN);
            assertThat(comment).isEqualTo("smoke comment");
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            deleteBranch(github, head);
        }
    }

    @Test
    void pullRequest_shouldBeMerged_whenItTargetsAThrowawayBranch() throws IOException {
        final GitHub github = ENV.github();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String main = repository.fetch().orElseThrow().record().defaultBranch();
        final String base = ENV.name("merge-base");
        final String head = ENV.name("merge-head");
        final String baseSha = branchSha(github, main);
        createBranch(github, base, baseSha);
        createBranch(github, head, baseSha);
        try {
            commitFile(github, head, "smoke/" + ENV.runId() + "-merge.txt");
            final PullRequest created = repository.createPullRequest(newPullRequest(head, base, ENV.name("merge")));

            final PullRequest merged = created.merge(MergeMethod.SQUASH);

            assertThat(created.isMerged()).isFalse();
            assertThat(merged.isMerged()).isTrue();
            assertThat(merged.state()).isEqualTo(PullRequestState.CLOSED);
        } finally {
            deleteBranch(github, head);
            deleteBranch(github, base);
        }
    }

    private static GitHubCreatePullRequestPayload newPullRequest(
            final String head, final String base, final String title) {
        return GitHubCreatePullRequestPayloadBuilder.builder()
                .head(head)
                .base(base)
                .title(title)
                .body("created by a smoke test")
                .build();
    }

    private static String branchSha(final GitHub github, final String branch) throws IOException {
        return github.gitDatabase()
                .refs()
                .getRef(ENV.owner(), ENV.repo(), "heads/" + branch)
                .orElseThrow()
                .object()
                .sha();
    }

    private static void createBranch(final GitHub github, final String branch, final String sha) throws IOException {
        github.gitDatabase()
                .refs()
                .createRef(ENV.owner(), ENV.repo(), new GitHubCreateRefPayload("refs/heads/" + branch, sha));
    }

    private static void deleteBranch(final GitHub github, final String branch) throws IOException {
        github.gitDatabase().refs().deleteRef(ENV.owner(), ENV.repo(), "heads/" + branch);
    }

    private static void commitFile(final GitHub github, final String branch, final String path) throws IOException {
        github.repositories()
                .contents()
                .createOrUpdateFileContents(
                        ENV.owner(),
                        ENV.repo(),
                        path,
                        new GitHubCreateOrUpdateFileContentsPayload(
                                "smoke commit",
                                Base64.getEncoder().encodeToString("smoke".getBytes(StandardCharsets.UTF_8)),
                                null,
                                branch,
                                null,
                                null));
    }
}
