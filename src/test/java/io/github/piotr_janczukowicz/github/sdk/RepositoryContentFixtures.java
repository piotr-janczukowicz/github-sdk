package io.github.piotr_janczukowicz.github.sdk;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionBuilder;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionLinks;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommitBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommitDetail;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFileBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFileType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentLinks;
import java.util.List;

final class RepositoryContentFixtures {

    private static final String API = "https://api.github.com/repos/octocat/hello-world";

    private RepositoryContentFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitHubCommit commit() {
        final GitHubCommitDetail detail = mock(GitHubCommitDetail.class);
        lenient().when(detail.message()).thenReturn("Fix the bug");
        return GitHubCommitBuilder.builder()
                .url(API + "/commits/abc123")
                .sha("abc123")
                .nodeId("C_kwDOA")
                .htmlUrl("https://github.com/octocat/hello-world/commit/abc123")
                .commentsUrl(API + "/commits/abc123/comments")
                .commit(detail)
                .parents(List.of())
                .build();
    }

    static GitHubContentFile contentFile(final String encoding, final String content) {
        return GitHubContentFileBuilder.builder()
                .type(GitHubContentFileType.FILE)
                .encoding(encoding)
                .size(5)
                .name("README.md")
                .path("docs/README.md")
                .content(content)
                .sha("blob123")
                .url(API + "/contents/docs/README.md")
                .links(mock(GitHubContentLinks.class))
                .build();
    }

    static GitHubBranchWithProtection branch() {
        return GitHubBranchWithProtectionBuilder.builder()
                .name("main")
                .commit(commit())
                .links(mock(GitHubBranchWithProtectionLinks.class))
                .protectedBranch(false)
                .protection(mock(GitHubBranchProtection.class))
                .protectionUrl(API + "/branches/main/protection")
                .build();
    }
}
