package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteStatus;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComment;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutolink;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;

public final class ExtrasFixtures {

    public static final long COMMENT_ID = 11L;
    public static final long SUITE_ID = 21L;
    public static final long AUTOLINK_ID = 31L;
    public static final String SHA = "a".repeat(40);

    private ExtrasFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubCommitComment commitComment() {
        return commitCommentWith(null, null);
    }

    public static GitHubCommitComment commitCommentWith(final String path, final Integer line) {
        final GitHubSimpleUser author = mock(GitHubSimpleUser.class);
        lenient().when(author.login()).thenReturn("octocat");
        final GitHubCommitComment comment = mock(GitHubCommitComment.class);
        lenient().when(comment.id()).thenReturn(COMMENT_ID);
        lenient().when(comment.body()).thenReturn("Nice");
        lenient().when(comment.commitId()).thenReturn(SHA);
        lenient().when(comment.user()).thenReturn(author);
        lenient().when(comment.path()).thenReturn(path);
        lenient().when(comment.line()).thenReturn(line);
        return comment;
    }

    public static GitHubCommitComment commitCommentWithoutAuthor() {
        final GitHubCommitComment comment = mock(GitHubCommitComment.class);
        lenient().when(comment.id()).thenReturn(COMMENT_ID);
        lenient().when(comment.user()).thenReturn(null);
        return comment;
    }

    public static GitHubCheckSuite checkSuite() {
        return checkSuiteWith("main", GitHubCheckSuiteStatus.COMPLETED, GitHubCheckSuiteConclusion.SUCCESS);
    }

    public static GitHubCheckSuite checkSuiteWith(
            final String branch, final GitHubCheckSuiteStatus status, final GitHubCheckSuiteConclusion conclusion) {
        final GitHubCheckSuite suite = mock(GitHubCheckSuite.class);
        lenient().when(suite.id()).thenReturn(SUITE_ID);
        lenient().when(suite.headSha()).thenReturn(SHA);
        lenient().when(suite.headBranch()).thenReturn(branch);
        lenient().when(suite.status()).thenReturn(status);
        lenient().when(suite.conclusion()).thenReturn(conclusion);
        return suite;
    }

    public static GitHubAutolink autolink() {
        return new GitHubAutolink(
                AUTOLINK_ID,
                "TICKET-",
                "https://tickets.example.com/<num>",
                false,
                Instant.parse("2026-11-05T10:00:00Z"));
    }
}
