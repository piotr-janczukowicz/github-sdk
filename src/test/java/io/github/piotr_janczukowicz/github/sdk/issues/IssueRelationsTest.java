package io.github.piotr_janczukowicz.github.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddBlockedByDependencyPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddSubIssuePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubRemoveSubIssuePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubReprioritizeSubIssuePayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueRelationsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 42;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubIssue response = IssuesFixtures.issue();
    private final Issue sut = new Issue(repository, response);
    private final GitHubIssue otherResponse = GitHubIssueBuilder.builder(response)
            .id(2002L)
            .number(7)
            .repositoryUrl("https://api.github.com/repos/hubot/linguist")
            .build();
    private final Issue other = new Issue(GitHubSdk.of(github).repository("hubot", "linguist"), otherResponse);

    @Test
    void parent_shouldBindTheParentToItsOwnRepository_whenItIsInAnotherRepository() throws IOException {
        when(github.issues().subIssues().getParent(OWNER, NAME, NUMBER)).thenReturn(Optional.of(otherResponse));

        final Optional<Issue> parent = sut.parent();

        assertThat(parent)
                .hasValueSatisfying(found -> assertSoftly(softly -> {
                    softly.assertThat(found.number()).isEqualTo(7);
                    softly.assertThat(found.repository().owner()).isEqualTo("hubot");
                    softly.assertThat(found.repository().name()).isEqualTo("linguist");
                }));
    }

    @Test
    void parent_shouldReturnEmpty_whenTheIssueHasNone() throws IOException {
        when(github.issues().subIssues().getParent(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Optional<Issue> parent = sut.parent();

        assertThat(parent).isEmpty();
    }

    @Test
    void subIssues_shouldKeepTheRepository_whenTheyAreInTheSameRepository() throws IOException {
        when(github.issues().subIssues().listSubIssues(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(response));

        final List<Issue> subIssues = sut.subIssues().toList();

        assertThat(subIssues)
                .singleElement()
                .satisfies(found -> assertThat(found.repository()).isSameAs(repository));
    }

    @Test
    void addSubIssue_shouldSendTheIdAndKeepTheParent_whenCalledWithAChild() throws IOException {
        when(github.issues().subIssues().addSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubAddSubIssuePayload> payload =
                ArgumentCaptor.forClass(GitHubAddSubIssuePayload.class);

        final Issue result = sut.addSubIssue(other);

        verify(github.issues().subIssues()).addSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().subIssueId()).isEqualTo(2002L);
            softly.assertThat(payload.getValue().replaceParent()).isFalse();
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void addSubIssue_shouldSendReplaceParent_whenAskedTo() throws IOException {
        when(github.issues().subIssues().addSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubAddSubIssuePayload> payload =
                ArgumentCaptor.forClass(GitHubAddSubIssuePayload.class);

        sut.addSubIssue(other, true);

        verify(github.issues().subIssues()).addSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertThat(payload.getValue().replaceParent()).isTrue();
    }

    @Test
    void addSubIssue_shouldThrowNullPointerException_whenTheChildIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addSubIssue(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("issue");
    }

    @Test
    void removeSubIssue_shouldSendTheId_whenCalled() throws IOException {
        when(github.issues().subIssues().removeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubRemoveSubIssuePayload> payload =
                ArgumentCaptor.forClass(GitHubRemoveSubIssuePayload.class);

        final Issue result = sut.removeSubIssue(other);

        verify(github.issues().subIssues()).removeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().subIssueId()).isEqualTo(2002L);
            softly.assertThat(result.number()).isEqualTo(NUMBER);
        });
    }

    @Test
    void moveSubIssueAfter_shouldSendAfterId_whenCalled() throws IOException {
        when(github.issues().subIssues().reprioritizeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubReprioritizeSubIssuePayload> payload =
                ArgumentCaptor.forClass(GitHubReprioritizeSubIssuePayload.class);

        sut.moveSubIssueAfter(other, sut);

        verify(github.issues().subIssues()).reprioritizeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().subIssueId()).isEqualTo(2002L);
            softly.assertThat(payload.getValue().afterId()).isEqualTo(1001L);
            softly.assertThat(payload.getValue().beforeId()).isNull();
        });
    }

    @Test
    void moveSubIssueBefore_shouldSendBeforeId_whenCalled() throws IOException {
        when(github.issues().subIssues().reprioritizeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubReprioritizeSubIssuePayload> payload =
                ArgumentCaptor.forClass(GitHubReprioritizeSubIssuePayload.class);

        sut.moveSubIssueBefore(other, sut);

        verify(github.issues().subIssues()).reprioritizeSubIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().beforeId()).isEqualTo(1001L);
            softly.assertThat(payload.getValue().afterId()).isNull();
        });
    }

    @Test
    void blockedBy_shouldStreamTheBlockingIssues_whenTheyExist() throws IOException {
        when(github.issues().issueDependencies().listDependenciesBlockedBy(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(otherResponse));

        final List<Issue> blockers = sut.blockedBy().toList();

        assertThat(blockers)
                .singleElement()
                .satisfies(found -> assertThat(found.repository().owner()).isEqualTo("hubot"));
    }

    @Test
    void blocking_shouldStreamTheBlockedIssues_whenTheyExist() throws IOException {
        when(github.issues().issueDependencies().listDependenciesBlocking(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(otherResponse));

        final List<Issue> blocked = sut.blocking().toList();

        assertThat(blocked)
                .singleElement()
                .satisfies(found -> assertThat(found.number()).isEqualTo(7));
    }

    @Test
    void addBlockedBy_shouldSendTheBlockerId_whenCalled() throws IOException {
        when(github.issues().issueDependencies().addBlockedByDependency(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(response);
        final ArgumentCaptor<GitHubAddBlockedByDependencyPayload> payload =
                ArgumentCaptor.forClass(GitHubAddBlockedByDependencyPayload.class);

        final Issue result = sut.addBlockedBy(other);

        verify(github.issues().issueDependencies())
                .addBlockedByDependency(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().issueId()).isEqualTo(2002L);
            softly.assertThat(result.repository()).isSameAs(repository);
        });
    }

    @Test
    void removeBlockedBy_shouldSendTheBlockerId_whenCalled() throws IOException {
        when(github.issues().issueDependencies().removeDependencyBlockedBy(OWNER, NAME, NUMBER, 2002L))
                .thenReturn(response);

        final Issue result = sut.removeBlockedBy(other);

        assertThat(result.number()).isEqualTo(NUMBER);
    }
}
