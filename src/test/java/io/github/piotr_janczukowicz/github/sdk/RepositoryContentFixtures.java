package io.github.piotr_janczukowicz.github.sdk;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionBuilder;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionLinks;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabelBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubRelease;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommitBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommitDetail;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFileBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFileType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentLinks;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneBuilder;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneState;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;
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

    static GitHubLabel label() {
        return GitHubLabelBuilder.builder()
                .id(4004L)
                .nodeId("LA_kwDOA")
                .url(API + "/labels/bug")
                .name("bug")
                .color("d73a4a")
                .isDefault(true)
                .build();
    }

    static GitHubMilestone milestone() {
        final Instant at = Instant.parse("2026-04-05T06:07:08Z");
        return GitHubMilestoneBuilder.builder()
                .url(API + "/milestones/3")
                .htmlUrl("https://github.com/octocat/hello-world/milestone/3")
                .labelsUrl(API + "/milestones/3/labels")
                .id(5005L)
                .nodeId("MI_kwDOA")
                .number(3)
                .state(GitHubMilestoneState.OPEN)
                .title("v1.0")
                .openIssues(2)
                .closedIssues(1)
                .createdAt(at)
                .updatedAt(at)
                .build();
    }

    static GitHubRelease release() {
        return GitHubReleaseBuilder.builder()
                .url(API + "/releases/9009")
                .htmlUrl("https://github.com/octocat/hello-world/releases/tag/v1.0.0")
                .assetsUrl(API + "/releases/9009/assets")
                .uploadUrl(API + "/releases/9009/assets{?name,label}")
                .id(9009L)
                .nodeId("RE_kwDOA")
                .tagName("v1.0.0")
                .targetCommitish("main")
                .draft(false)
                .prerelease(false)
                .createdAt(Instant.parse("2026-03-04T05:06:07Z"))
                .author(mock(GitHubSimpleUser.class))
                .assets(List.of())
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
