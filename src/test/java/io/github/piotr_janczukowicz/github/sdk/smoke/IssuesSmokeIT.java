package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateLabelPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubListForRepoFilter;
import io.github.piotr_janczukowicz.github.api.issues.GitHubListForRepoFilterBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubListIssuesState;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.IssueComment;
import io.github.piotr_janczukowicz.github.sdk.issues.IssueState;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class IssuesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void fetch_shouldReturnTheSandbox_whenTokenHasAccess() throws IOException {
        final Repository handle = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Repository> loaded = handle.fetch();

        assertThat(loaded).map(repository -> repository.response().fullName()).contains(ENV.owner() + "/" + ENV.repo());
    }

    @Test
    void issue_shouldRoundTrip_whenCreatedCommentedEditedClosedAndReopened() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        Issue issue = repository.createIssue(ENV.name("issue"));
        IssueComment comment = null;
        try {
            comment = issue.comment("smoke comment");
            comment = comment.edit("smoke comment, edited");
            final List<IssueComment> comments = issue.comments().toList();
            final Issue closed = issue.close();
            issue = closed.reopen();

            final Optional<Issue> reloaded = repository.issue(issue.number());

            final IssueComment edited = comment;
            final Issue reopened = issue;
            assertSoftly(softly -> {
                softly.assertThat(closed.state()).isEqualTo(IssueState.CLOSED);
                softly.assertThat(reopened.state()).isEqualTo(IssueState.OPEN);
                softly.assertThat(comments).extracting(IssueComment::id).contains(edited.id());
                softly.assertThat(edited.body()).contains("smoke comment, edited");
                softly.assertThat(reloaded).map(Issue::title).contains(ENV.name("issue"));
            });
        } finally {
            if (comment != null) {
                comment.delete();
            }
            issue.close();
        }
    }

    @Test
    void update_shouldChangeTitleAndBody_whenSentAndRefreshed() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue created = repository.createIssue(ENV.name("update"));
        try {
            final Issue updated = created.update()
                    .title(ENV.name("updated"))
                    .body("changed by a smoke test")
                    .send();
            final Issue refreshed = updated.refresh();

            assertSoftly(softly -> {
                softly.assertThat(updated.title()).isEqualTo(ENV.name("updated"));
                softly.assertThat(refreshed.response().body()).isEqualTo("changed by a smoke test");
                softly.assertThat(created.title()).isEqualTo(ENV.name("update"));
            });
        } finally {
            created.close();
        }
    }

    @Test
    void labels_shouldRoundTrip_whenAddedSetAndRemoved() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String labelName = ENV.name("label");
        ENV.github()
                .issues()
                .labels()
                .createLabel(ENV.owner(), ENV.repo(), new GitHubCreateLabelPayload(labelName, "ededed", null));
        final Issue created = repository.createIssue(ENV.name("labels"));
        try {
            final Issue added = created.addLabels(labelName);
            final Issue removed = added.removeLabel(labelName);
            final Issue set = removed.setLabels(List.of(labelName));

            assertSoftly(softly -> {
                softly.assertThat(added.labels())
                        .extracting(GitHubIssueLabel::name)
                        .contains(labelName);
                softly.assertThat(removed.labels()).isEmpty();
                softly.assertThat(set.labels())
                        .extracting(GitHubIssueLabel::name)
                        .containsExactly(labelName);
            });
        } finally {
            created.close();
            ENV.github().issues().labels().deleteLabel(ENV.owner(), ENV.repo(), labelName);
        }
    }

    @Test
    void lock_shouldRoundTrip_whenLockedAndUnlocked() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue created = repository.createIssue(ENV.name("lock"));
        try {
            final Issue locked = created.lock();
            final Issue unlocked = locked.unlock();

            assertSoftly(softly -> {
                softly.assertThat(locked.response().locked()).isTrue();
                softly.assertThat(unlocked.response().locked()).isFalse();
            });
        } finally {
            created.close();
        }
    }

    @Test
    void issues_shouldListExistingIssuesOfTheRepository_whenTheyAreLoadedByNumber() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final GitHubListForRepoFilter all = GitHubListForRepoFilterBuilder.builder()
                .state(GitHubListIssuesState.ALL)
                .build();

        final List<Issue> issues = repository.issues(all).limit(5).toList();
        Assumptions.assumeFalse(issues.isEmpty(), "the sandbox repository has no issues");
        final Issue first = issues.getFirst();

        final Optional<Issue> loaded = repository.issue(first.number());

        assertSoftly(softly -> {
            softly.assertThat(issues)
                    .allSatisfy(issue -> assertThat(issue.repository()).isSameAs(repository));
            softly.assertThat(loaded).map(Issue::title).contains(first.title());
        });
    }

    @Test
    void issue_shouldBeEmpty_whenTheNumberDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Issue> issue = repository.issue(Integer.MAX_VALUE);

        assertThat(issue).isEmpty();
    }
}
