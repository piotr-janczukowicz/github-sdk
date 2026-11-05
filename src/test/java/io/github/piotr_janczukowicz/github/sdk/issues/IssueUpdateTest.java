package io.github.piotr_janczukowicz.github.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueMilestoneReference;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateIssueAssignee;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateIssueLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 42;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubIssue response = IssuesFixtures.issue();
    private final Issue issue = new Issue(GitHubSdk.of(github).repository(OWNER, NAME), response);

    @Test
    void send_shouldSendOnlyTheSetFields_whenTitleAndLabelsAreSet() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        issue.update().title("New title").labels(List.of("bug")).send();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo(OptionalNull.of("New title"));
            softly.assertThat(payload.getValue().labels()).containsExactly(GitHubUpdateIssueLabel.ofName("bug"));
            softly.assertThat(payload.getValue().body()).isNull();
            softly.assertThat(payload.getValue().assignees()).isNull();
            softly.assertThat(payload.getValue().milestone()).isNull();
        });
    }

    @Test
    void send_shouldSendAssigneesAndMilestone_whenTheyAreSet() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        issue.update().assignees(List.of("hubot")).milestone(7).body("Details").send();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().assignees())
                    .containsExactly(GitHubUpdateIssueAssignee.ofLogin("hubot"));
            softly.assertThat(payload.getValue().milestone())
                    .isEqualTo(OptionalNull.of(GitHubIssueMilestoneReference.ofNumber(7)));
            softly.assertThat(payload.getValue().body()).isEqualTo(OptionalNull.of("Details"));
        });
    }

    @Test
    void send_shouldSendNullBodyAndMilestone_whenTheyAreCleared() throws IOException {
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(response));
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        issue.update().clearBody().clearMilestone().send();

        verify(github.issues().issue()).update(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(payload.getValue().milestone()).isEqualTo(OptionalNull.ofNull());
        });
    }

    @Test
    void send_shouldReturnTheReloadedIssue_whenTheUpdateSucceeds() throws IOException {
        final GitHubIssue reloaded =
                GitHubIssueBuilder.builder(response).title("New title").build();
        when(github.issues().issue().get(OWNER, NAME, NUMBER)).thenReturn(Optional.of(reloaded));

        final Issue updated = issue.update().title("New title").send();

        assertSoftly(softly -> {
            softly.assertThat(updated.response()).isSameAs(reloaded);
            softly.assertThat(issue.title()).isEqualTo("Found a bug");
        });
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final IssueUpdate update = issue.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.issues().issue());
    }

    @Test
    void title_shouldThrowNullPointerException_whenTitleIsNull() {
        final IssueUpdate update = issue.update();

        final Throwable thrown = catchThrowable(() -> update.title(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("title");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenIssueIsNull() {
        final Throwable thrown = catchThrowable(() -> new IssueUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("issue");
    }

    @Test
    void labelsAndAssignees_shouldThrowIllegalArgumentException_whenAValueIsBlank() {
        final IssueUpdate update = issue.update();

        final Throwable labels = catchThrowable(() -> update.labels(List.of(" ")));
        final Throwable assignees = catchThrowable(() -> update.assignees(List.of(" ")));

        assertSoftly(softly -> {
            softly.assertThat(labels).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(assignees).isInstanceOf(IllegalArgumentException.class);
        });
    }
}
