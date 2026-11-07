package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayloadBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestUpdateState;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubUpdatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PullRequestsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void pullRequest_shouldBeCreatedReadAndListed_whenABranchHasACommit() throws IOException {
        final GitHub github = ENV.github();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String branch = ENV.name("pr");
        final String path = "smoke/" + ENV.runId() + "-sdk-pr.txt";
        final String sha = github.gitDatabase()
                .refs()
                .getRef(ENV.owner(), ENV.repo(), "heads/" + base)
                .orElseThrow()
                .object()
                .sha();
        github.gitDatabase()
                .refs()
                .createRef(ENV.owner(), ENV.repo(), new GitHubCreateRefPayload("refs/heads/" + branch, sha));
        PullRequest created = null;
        try {
            github.repositories()
                    .contents()
                    .createOrUpdateFileContents(
                            ENV.owner(),
                            ENV.repo(),
                            path,
                            new GitHubCreateOrUpdateFileContentsPayload(
                                    "smoke pr",
                                    Base64.getEncoder().encodeToString("pr".getBytes(StandardCharsets.UTF_8)),
                                    null,
                                    branch,
                                    null,
                                    null));
            created = repository.createPullRequest(GitHubCreatePullRequestPayloadBuilder.builder()
                    .head(branch)
                    .base(base)
                    .title(ENV.name("pr"))
                    .body("created by a smoke test")
                    .build());

            final Optional<PullRequest> read = repository.pullRequest(created.number());
            final boolean isListed = repository
                    .pullRequests()
                    .limit(50)
                    .anyMatch(listed -> listed.number() == read.orElseThrow().number());

            final PullRequest loaded = read.orElseThrow();
            final List<String> files =
                    loaded.files().map(GitHubDiffEntry::filename).toList();
            final long commits = loaded.commits().count();

            final PullRequest opened = created;
            assertSoftly(softly -> {
                softly.assertThat(opened.state()).isEqualTo(PullRequestState.OPEN);
                softly.assertThat(read).map(PullRequest::title).contains(ENV.name("pr"));
                softly.assertThat(isListed).isTrue();
                softly.assertThat(files).contains(path);
                softly.assertThat(commits).isEqualTo(1);
            });
        } finally {
            if (created != null) {
                github.pullRequests()
                        .pullRequest()
                        .update(
                                ENV.owner(),
                                ENV.repo(),
                                created.number(),
                                new GitHubUpdatePullRequestPayload(
                                        null, null, GitHubPullRequestUpdateState.CLOSED, null, null));
            }
            github.gitDatabase().refs().deleteRef(ENV.owner(), ENV.repo(), "heads/" + branch);
        }
    }

    @Test
    void pullRequest_shouldBeEmpty_whenTheNumberDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<PullRequest> pullRequest = repository.pullRequest(Integer.MAX_VALUE);

        assertThat(pullRequest).isEmpty();
    }
}
