package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.sdk.Issue;
import io.github.piotr_janczukowicz.github.sdk.IssueComment;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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

        assertThat(loaded).map(repository -> repository.record().fullName()).contains(ENV.owner() + "/" + ENV.repo());
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

            assertThat(closed.state()).isEqualTo("closed");
            assertThat(issue.state()).isEqualTo("open");
            assertThat(comments).extracting(IssueComment::id).contains(comment.id());
            assertThat(comment.body()).contains("smoke comment, edited");
            assertThat(repository.issue(issue.number())).map(Issue::title).contains(ENV.name("issue"));
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

            assertThat(updated.title()).isEqualTo(ENV.name("updated"));
            assertThat(refreshed.record().body()).isEqualTo("changed by a smoke test");
            assertThat(created.title()).isEqualTo(ENV.name("update"));
        } finally {
            created.close();
        }
    }

    @Test
    void issue_shouldBeEmpty_whenTheNumberDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Issue> issue = repository.issue(Integer.MAX_VALUE);

        assertThat(issue).isEmpty();
    }
}
