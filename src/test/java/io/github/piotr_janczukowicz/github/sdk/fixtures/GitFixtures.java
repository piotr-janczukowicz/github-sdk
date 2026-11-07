package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparison;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparisonStatus;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRef;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitRefObject;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCommit;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import java.util.List;

public final class GitFixtures {

    public static final String SHA = "a".repeat(40);
    public static final String NEW_SHA = "d".repeat(40);

    private static final String API = "https://api.github.com/repos/octocat/hello-world/git/refs/heads/main";

    private GitFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubGitRef ref() {
        return refWith("refs/heads/main", SHA);
    }

    public static GitHubGitRef refWith(final String name, final String sha) {
        return new GitHubGitRef(name, "REF_kwDOA", API, new GitHubGitRefObject("commit", sha, API));
    }

    public static GitHubCommitComparison comparison() {
        final GitHubCommitComparison comparison = mock(GitHubCommitComparison.class);
        final List<GitHubCommit> commits = List.of(RepositoryContentFixtures.commit());
        final List<GitHubDiffEntry> files = List.of(mock(GitHubDiffEntry.class));
        lenient().when(comparison.status()).thenReturn(GitHubCommitComparisonStatus.AHEAD);
        lenient().when(comparison.aheadBy()).thenReturn(2);
        lenient().when(comparison.behindBy()).thenReturn(0);
        lenient().when(comparison.totalCommits()).thenReturn(2);
        lenient().when(comparison.commits()).thenReturn(commits);
        lenient().when(comparison.files()).thenReturn(files);
        return comparison;
    }
}
