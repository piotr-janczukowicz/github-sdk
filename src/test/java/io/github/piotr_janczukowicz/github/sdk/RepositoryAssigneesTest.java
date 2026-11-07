package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryAssigneesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void assignees_shouldStreamTheUsers_whenTheyExist() throws IOException {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        when(github.issues().assignees().listAssignees(OWNER, NAME).stream()).thenReturn(Stream.of(user));

        final List<GitHubSimpleUser> assignees = sut.assignees().toList();

        assertThat(assignees).containsExactly(user);
    }

    @Test
    void canBeAssigned_shouldReturnTrue_whenGitHubAnswersYes() throws IOException {
        when(github.issues().assignees().checkUserCanBeAssigned(OWNER, NAME, "hubot"))
                .thenReturn(true);

        final boolean isAssignable = sut.canBeAssigned("hubot");

        assertThat(isAssignable).isTrue();
    }

    @Test
    void canBeAssigned_shouldReturnFalse_whenGitHubAnswersNo() throws IOException {
        when(github.issues().assignees().checkUserCanBeAssigned(OWNER, NAME, "ghost"))
                .thenReturn(false);

        final boolean isAssignable = sut.canBeAssigned("ghost");

        assertThat(isAssignable).isFalse();
    }

    @Test
    void canBeAssigned_shouldThrowNullPointerException_whenTheLoginIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.canBeAssigned(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("login");
    }

    @Test
    void canBeAssigned_shouldThrowIllegalArgumentException_whenTheLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.canBeAssigned(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void issueOf_shouldKeepTheRepository_whenTheIssueIsInThisRepository() {
        final Issue issue = sut.issueOf(IssuesFixtures.issue());

        assertThat(issue.repository()).isSameAs(sut);
    }

    @Test
    void issueOf_shouldBindAnotherRepository_whenTheIssueIsInAnotherRepository() {
        final GitHubIssue foreign = GitHubIssueBuilder.builder(IssuesFixtures.issue())
                .repositoryUrl("https://api.github.com/repos/hubot/linguist")
                .build();

        final Issue issue = sut.issueOf(foreign);

        assertSoftly(softly -> {
            softly.assertThat(issue.repository().owner()).isEqualTo("hubot");
            softly.assertThat(issue.repository().name()).isEqualTo("linguist");
        });
    }

    @Test
    void issueOf_shouldBindAnotherRepository_whenOnlyTheNameDiffers() {
        final GitHubIssue sibling = GitHubIssueBuilder.builder(IssuesFixtures.issue())
                .repositoryUrl("https://api.github.com/repos/octocat/linguist")
                .build();

        final Issue issue = sut.issueOf(sibling);

        assertThat(issue.repository().name()).isEqualTo("linguist");
    }

    @Test
    void issueOf_shouldThrowNullPointerException_whenTheResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.issueOf(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void issueOf_shouldBindAnotherRepository_whenOnlyTheOwnerDiffers() {
        final GitHubIssue sibling = GitHubIssueBuilder.builder(IssuesFixtures.issue())
                .repositoryUrl("https://api.github.com/repos/hubot/hello-world")
                .build();

        final Issue issue = sut.issueOf(sibling);

        assertThat(issue.repository().owner()).isEqualTo("hubot");
    }

    @Test
    void issueOf_shouldThrowIllegalArgumentException_whenTheRepositoryUrlIsMalformed() {
        final GitHubIssue malformed = GitHubIssueBuilder.builder(IssuesFixtures.issue())
                .repositoryUrl("hello-world")
                .build();

        final Throwable thrown = catchThrowable(() -> sut.issueOf(malformed));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hello-world");
    }
}
