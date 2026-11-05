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
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddAssigneesAssignee;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddAssigneesPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddLabelsLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddLabelsPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueUpdateState;
import io.github.piotr_janczukowicz.github.api.issues.GitHubRemoveAssigneesPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubSetLabelsLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubSetLabelsPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubIssueComment;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 42;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubIssue response = IssuesFixtures.issue();
    private final Issue sut = new Issue(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenIssueIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.title()).isEqualTo("Found a bug");
            softly.assertThat(sut.state()).isEqualTo(IssueState.OPEN);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void author_shouldBeEmpty_whenTheResponseHasNoUser() {
        final Optional<GitHubSimpleUser> author = sut.author();

        assertThat(author).isEmpty();
    }

    @Test
    void author_shouldReturnTheUser_whenTheResponseHasOne() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        final Issue withAuthor = new Issue(
                repository, GitHubIssueBuilder.builder(response).user(user).build());

        final Optional<GitHubSimpleUser> author = withAuthor.author();

        assertThat(author).containsSame(user);
    }

    @Test
    void close_shouldSendClosedAndReturnTheReloadedIssue_whenUpdateSucceeds() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).state("closed").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Issue closed = sut.close();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubIssueUpdateState.CLOSED);
            softly.assertThat(closed.response()).isSameAs(reloaded);
            softly.assertThat(sut.state()).isEqualTo(IssueState.OPEN);
        });
    }

    @Test
    void reopen_shouldSendOpenAndReturnTheReloadedIssue_whenUpdateSucceeds() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).state("open").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Issue reopened = sut.reopen();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubIssueUpdateState.OPEN);
            softly.assertThat(reopened.response()).isSameAs(reloaded);
        });
    }

    @Test
    void close_shouldThrowIllegalStateException_whenTheIssueIsGoneAfterTheUpdate() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::close);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void refresh_shouldReturnTheReloadedIssue_whenGitHubHasIt() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).title("Renamed").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final Issue refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
            softly.assertThat(sut.title()).isEqualTo("Found a bug");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheIssueIsGone() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void labels_shouldBeEmpty_whenTheResponseHasNone() {
        final List<GitHubIssueLabel> labels = sut.labels();

        assertThat(labels).isEmpty();
    }

    @Test
    void labels_shouldReturnTheLabelsOfTheResponse_whenTheyArePresent() {
        final GitHubIssueLabel label = mock(GitHubIssueLabel.class);
        final Issue labelled = new Issue(
                repository,
                GitHubIssueBuilder.builder(response).labels(List.of(label)).build());

        final List<GitHubIssueLabel> labels = labelled.labels();

        assertThat(labels).containsExactly(label);
    }

    @Test
    void milestone_shouldBeEmpty_whenTheResponseHasNone() {
        final Optional<Milestone> milestone = sut.milestone();

        assertThat(milestone).isEmpty();
    }

    @Test
    void milestone_shouldWrapTheMilestoneOfTheResponse_whenTheResponseHasOne() {
        final GitHubMilestone milestoneResponse = RepositoryContentFixtures.milestone();
        final Issue planned = new Issue(
                repository,
                GitHubIssueBuilder.builder(response)
                        .milestone(milestoneResponse)
                        .build());

        final Optional<Milestone> milestone = planned.milestone();

        assertThat(milestone).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(milestone.orElseThrow().response()).isSameAs(milestoneResponse);
            softly.assertThat(milestone.orElseThrow().repository()).isSameAs(repository);
        });
    }

    @Test
    void assignees_shouldBeEmpty_whenTheResponseHasNone() {
        final List<GitHubSimpleUser> assignees = sut.assignees();

        assertThat(assignees).isEmpty();
    }

    @Test
    void assignees_shouldReturnTheUsersOfTheResponse_whenTheyArePresent() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        final Issue assigned = new Issue(
                repository,
                GitHubIssueBuilder.builder(response).assignees(List.of(user)).build());

        final List<GitHubSimpleUser> assignees = assigned.assignees();

        assertThat(assignees).containsExactly(user);
    }

    @Test
    void addLabels_shouldSendTheNamesAndReturnTheReloadedIssue_whenCalled() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).comments(1).build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubAddLabelsPayload> payload = ArgumentCaptor.forClass(GitHubAddLabelsPayload.class);

        final Issue labelled = sut.addLabels("bug", "help wanted");

        verify(github.issues().labels()).addLabels(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().labels())
                    .containsExactly(GitHubAddLabelsLabel.ofName("bug"), GitHubAddLabelsLabel.ofName("help wanted"));
            softly.assertThat(labelled.response()).isSameAs(reloaded);
        });
    }

    @Test
    void addLabels_shouldThrowIllegalArgumentException_whenNoNameIsGiven() {
        final Throwable thrown = catchThrowable(sut::addLabels);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("names");
    }

    @Test
    void setLabels_shouldSendTheNamesAndReturnTheReloadedIssue_whenCalled() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).comments(1).build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));
        final ArgumentCaptor<GitHubSetLabelsPayload> payload = ArgumentCaptor.forClass(GitHubSetLabelsPayload.class);

        final Issue labelled = sut.setLabels(List.of("bug"));

        verify(github.issues().labels()).setLabels(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().labels()).containsExactly(GitHubSetLabelsLabel.ofName("bug"));
            softly.assertThat(labelled.response()).isSameAs(reloaded);
        });
    }

    @Test
    void removeLabel_shouldRemoveTheLabelAndReturnTheReloadedIssue_whenCalled() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).comments(1).build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final Issue unlabelled = sut.removeLabel("bug");

        verify(github.issues().labels()).removeLabel(OWNER, NAME, NUMBER, "bug");
        assertThat(unlabelled.response()).isSameAs(reloaded);
    }

    @Test
    void assign_shouldSendTheLoginsAndReturnTheResponse_whenCalled() throws IOException {
        final GitHubIssue assigned =
                GitHubIssueBuilder.builder(response).comments(1).build();
        when(github.issues()
                        .assignees()
                        .addAssignees(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubAddAssigneesPayload.class)))
                .thenReturn(assigned);
        final ArgumentCaptor<GitHubAddAssigneesPayload> payload =
                ArgumentCaptor.forClass(GitHubAddAssigneesPayload.class);

        final Issue result = sut.assign("hubot");

        verify(github.issues().assignees()).addAssignees(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().assignees())
                    .containsExactly(GitHubAddAssigneesAssignee.ofLogin("hubot"));
            softly.assertThat(result.response()).isSameAs(assigned);
        });
    }

    @Test
    void assign_shouldThrowIllegalArgumentException_whenNoLoginIsGiven() {
        final Throwable thrown = catchThrowable(sut::assign);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("logins");
    }

    @Test
    void unassign_shouldSendTheLoginsAndReturnTheResponse_whenCalled() throws IOException {
        final GitHubIssue unassigned =
                GitHubIssueBuilder.builder(response).comments(1).build();
        when(github.issues()
                        .assignees()
                        .removeAssignees(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubRemoveAssigneesPayload.class)))
                .thenReturn(unassigned);
        final ArgumentCaptor<GitHubRemoveAssigneesPayload> payload =
                ArgumentCaptor.forClass(GitHubRemoveAssigneesPayload.class);

        final Issue result = sut.unassign("hubot");

        verify(github.issues().assignees()).removeAssignees(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().assignees()).containsExactly("hubot");
            softly.assertThat(result.response()).isSameAs(unassigned);
        });
    }

    @Test
    void lock_shouldLockTheIssueAndReturnTheReloadedIssue_whenCalled() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).locked(true).build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final Issue locked = sut.lock();

        verify(github.issues().issue()).lock(OWNER, NAME, NUMBER);
        assertThat(locked.response()).isSameAs(reloaded);
    }

    @Test
    void unlock_shouldUnlockTheIssueAndReturnTheReloadedIssue_whenCalled() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).locked(false).build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final Issue unlocked = sut.unlock();

        verify(github.issues().issue()).unlock(OWNER, NAME, NUMBER);
        assertThat(unlocked.response()).isSameAs(reloaded);
    }

    @Test
    void comment_shouldSendTheBodyAndReturnTheComment_whenCalledWithABody() throws IOException {
        final GitHubIssueComment created = IssuesFixtures.issueComment();
        when(github.issues()
                        .comments()
                        .createComment(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubCreateCommentPayload.class)))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCommentPayload.class);

        final IssueComment comment = sut.comment("First comment");

        verify(github.issues().comments()).createComment(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("First comment");
            softly.assertThat(comment.response()).isSameAs(created);
            softly.assertThat(comment.issue()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.comment(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("body");
    }

    @Test
    void comments_shouldWrapEachComment_whenTheIssueHasComments() throws IOException {
        final GitHubIssueComment first = IssuesFixtures.issueComment();
        when(github.issues().comments().listComments(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(first));

        final List<IssueComment> comments = sut.comments().toList();

        assertThat(comments).hasSize(1);
        assertSoftly(softly -> {
            softly.assertThat(comments.getFirst().response()).isSameAs(first);
            softly.assertThat(comments.getFirst().issue()).isSameAs(sut);
        });
    }

    @Test
    void varargs_shouldThrowIllegalArgumentException_whenAValueIsBlank() {
        final Throwable label = catchThrowable(() -> sut.addLabels("bug", " "));
        final Throwable assignee = catchThrowable(() -> sut.assign(" "));
        final Throwable unassignee = catchThrowable(() -> sut.unassign("hubot", " "));

        assertSoftly(softly -> {
            softly.assertThat(label)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("names");
            softly.assertThat(assignee)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("logins");
            softly.assertThat(unassignee)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("logins");
        });
    }

    @Test
    void varargs_shouldThrowNullPointerException_whenAValueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.assign("hubot", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("logins");
    }

    @Test
    void setLabels_shouldThrowIllegalArgumentException_whenALabelIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.setLabels(List.of("bug", " ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("names");
    }
}
