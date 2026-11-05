package io.github.piotr_janczukowicz.github.sdk.smoke;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayloadBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class SandboxBranches {

    private SandboxBranches() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitHubCreatePullRequestPayload newPullRequest(final String head, final String base, final String title) {
        return GitHubCreatePullRequestPayloadBuilder.builder()
                .head(head)
                .base(base)
                .title(title)
                .body("created by a smoke test")
                .build();
    }

    static String branchSha(final SmokeEnvironment env, final String branch) throws IOException {
        return env.github()
                .gitDatabase()
                .refs()
                .getRef(env.owner(), env.repo(), "heads/" + branch)
                .orElseThrow()
                .object()
                .sha();
    }

    static void createBranch(final SmokeEnvironment env, final String branch, final String sha) throws IOException {
        env.github()
                .gitDatabase()
                .refs()
                .createRef(env.owner(), env.repo(), new GitHubCreateRefPayload("refs/heads/" + branch, sha));
    }

    static void deleteBranch(final SmokeEnvironment env, final String branch) throws IOException {
        final GitHub github = env.github();
        github.gitDatabase().refs().deleteRef(env.owner(), env.repo(), "heads/" + branch);
    }

    static void commitFile(final SmokeEnvironment env, final String branch, final String path) throws IOException {
        env.github()
                .repositories()
                .contents()
                .createOrUpdateFileContents(
                        env.owner(),
                        env.repo(),
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
